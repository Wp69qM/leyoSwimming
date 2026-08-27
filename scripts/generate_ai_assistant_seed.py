#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 AI 助理前置种子数据 SQL。
产出：scripts/seed/ai-assistant-seed.sql
数据规模：教练 10 名、套餐模板 10 个、用户 20 名，每个用户 1 条已购套餐 + 1 条已支付订单。
"""
import base64
import hashlib
import hmac
import json
import os
import random
from datetime import datetime, timedelta

from cryptography.hazmat.primitives import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

OUTPUT = os.path.join(os.path.dirname(__file__), "seed", "ai-assistant-seed.sql")

# 与 backend deploy/.env 中 PHONE_ENCRYPTION_KEY 保持一致
PHONE_ENCRYPTION_KEY = "dev-phone-encryption-key-change-in-production"
_KEY_BYTES = hashlib.sha256(PHONE_ENCRYPTION_KEY.encode("utf-8")).digest()

# 本地 uploads 目录，用于生成真实可访问的头像 URL
BASE_UPLOAD_URL = "http://localhost:8080/uploads"


def _encrypt_phone(phone: str) -> str:
    """AES/CBC/PKCS5Padding，格式：base64(iv):base64(ciphertext)"""
    iv = os.urandom(16)
    padder = padding.PKCS7(algorithms.AES.block_size).padder()
    padded = padder.update(phone.encode("utf-8")) + padder.finalize()
    cipher = Cipher(algorithms.AES(_KEY_BYTES), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded) + encryptor.finalize()
    return base64.b64encode(iv).decode("utf-8") + ":" + base64.b64encode(ciphertext).decode("utf-8")


def _hash_phone(phone: str) -> str:
    """HMAC-SHA256，base64 编码"""
    return base64.b64encode(
        hmac.new(_KEY_BYTES, phone.encode("utf-8"), hashlib.sha256).digest()
    ).decode("utf-8")


def _make_user_phone(index: int) -> str:
    """生成 20 个互不相同的真实格式手机号"""
    return f"138{(index + 1) % 100000000:08d}"


def _make_coach_phone(index: int) -> str:
    """生成 10 个互不相同的真实格式手机号，与用户号段错开"""
    return f"139{(index + 1) % 100000000:08d}"


def _make_id_card(index: int, gender: str) -> str:
    """生成 18 位虚拟身份证号"""
    # 前 6 位地址码：110101（北京市东城区）
    # 出生日期：1990 + index % 20 年，01-12 月，01-28 日
    year = 1990 + (index % 20)
    month = 1 + (index % 12)
    day = 1 + (index % 28)
    birth = f"{year}{month:02d}{day:02d}"
    # 顺序码，性别：奇数为男，偶数为女
    seq = 100 + index * 2
    if gender == "male":
        seq += 1
    seq_str = f"{seq:03d}"
    # 前 17 位
    base = f"110101{birth}{seq_str}"
    # 计算校验码
    weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
    check_codes = "10X98765432"
    total = sum(int(base[i]) * weights[i] for i in range(17))
    return base + check_codes[total % 11]


def _load_upload_images() -> list:
    """加载项目根目录 uploads/ 下的图片文件，返回完整 URL 列表"""
    upload_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")
    if not os.path.isdir(upload_dir):
        return []
    exts = {".png", ".jpg", ".jpeg", ".webp"}
    files = [f for f in os.listdir(upload_dir) if os.path.splitext(f.lower())[1] in exts]
    files.sort()
    return [f"{BASE_UPLOAD_URL}/{f}" for f in files]


def _avatar_url(images: list, index: int) -> str:
    """按顺序循环分配上传目录中的图片作为头像"""
    if not images:
        return ""
    return images[index % len(images)]


COACH_COUNT = 10
TEMPLATE_COUNT = 10
USER_COUNT = 20

COACH_NAMES = [
    "李教练", "王教练", "张教练", "刘教练", "陈教练",
    "杨教练", "黄教练", "赵教练", "周教练", "吴教练",
]

USER_NAMES = [
    "张明", "李华", "王芳", "刘洋", "陈静",
    "杨强", "黄丽", "赵军", "周敏", "吴磊",
    "徐涛", "孙倩", "马超", "朱琳", "胡勇",
    "郭娟", "何伟", "罗霞", "高鹏", "林雪",
]

# 每位教练的差异化特点描述，供 AI 推荐时作为选择依据
COACH_BIOS = [
    "国家级健将运动员出身，擅长自由泳技术细节纠正，对怕水学员有耐心，曾帮助 50+ 零基础学员完成首泳。",
    "儿童游泳教学专家，课堂游戏化设计，擅长用趣味方式让孩子克服恐水，家长满意度 4.9+。",
    "主攻蛙泳提速与动作优化，擅长中考游泳冲刺训练，熟悉各地中考评分标准。",
    "资深仰泳教练，注重身体平衡与呼吸节奏，适合希望改善体态和肩颈问题的成人学员。",
    "蝶泳专项教练，力量训练与游泳技术结合，适合有基础、想挑战竞技泳姿的学员。",
    "女性教练，擅长亲子共学与一对三小班，课堂氛围轻松，沟通细致温和。",
    "退役省队教练，带训严格、计划性强，适合目标明确、希望短期突破的成人学员。",
    "康复游泳方向，擅长为运动损伤、腰椎肩颈不适人群设计低冲击训练方案。",
    "长距离耐力训练专家，多次带队参加公开水域活动，适合想提升心肺功能的学员。",
    "零基础启蒙金牌教练，教学步骤拆分极细，从憋气到完整动作循序渐进，学员留存率高。",
]

TEMPLATE_SPECS = [
    {"name": "成人一对一私教10节", "mode": "standard", "teaching": "one_on_one", "hours": 10, "duration": 60, "days": 90, "original": 5000.00, "price": 3999.00, "strokes": [1, 2], "tags": ["成人", "一对一"], "desc": "适合成人的一对一私教课程，自由泳/蛙泳任选。"},
    {"name": "儿童一对二小班8节", "mode": "standard", "teaching": "one_on_two", "hours": 8, "duration": 60, "days": 60, "original": 3200.00, "price": 2599.00, "strokes": [1, 3], "tags": ["儿童", "小班"], "desc": "儿童一对二小班教学，趣味性强。"},
    {"name": "零基础体验课2节", "mode": "experience", "teaching": "one_on_one", "hours": 2, "duration": 45, "days": 14, "original": 600.00, "price": 99.00, "strokes": [2], "tags": ["体验", "零基础"], "desc": "零基础体验课，快速克服怕水心理。"},
    {"name": "仰泳专项提升6节", "mode": "standard", "teaching": "one_on_one", "hours": 6, "duration": 60, "days": 45, "original": 3000.00, "price": 2399.00, "strokes": [3], "tags": ["专项", "仰泳"], "desc": "针对仰泳动作细节进行专项提升。"},
    {"name": "蝶泳进阶私教8节", "mode": "standard", "teaching": "one_on_one", "hours": 8, "duration": 60, "days": 60, "original": 4800.00, "price": 3899.00, "strokes": [4], "tags": ["进阶", "蝶泳"], "desc": "蝶泳技术进阶，强化力量与节奏。"},
    {"name": "亲子一对三欢乐课12节", "mode": "standard", "teaching": "one_on_three", "hours": 12, "duration": 60, "days": 90, "original": 4800.00, "price": 3599.00, "strokes": [1, 2, 3], "tags": ["亲子", "一对三"], "desc": "亲子一同学习，一对三小班欢乐课。"},
    {"name": "中考游泳冲刺10节", "mode": "standard", "teaching": "one_on_two", "hours": 10, "duration": 60, "days": 60, "original": 4000.00, "price": 2999.00, "strokes": [1, 2], "tags": ["中考", "冲刺"], "desc": "针对中考游泳项目的冲刺训练。"},
    {"name": "成人自由泳速成5节", "mode": "standard", "teaching": "one_on_one", "hours": 5, "duration": 60, "days": 30, "original": 2500.00, "price": 1899.00, "strokes": [1], "tags": ["速成", "自由泳"], "desc": "5节课掌握自由泳基础动作。"},
    {"name": "儿童蛙泳启蒙6节", "mode": "standard", "teaching": "one_on_two", "hours": 6, "duration": 45, "days": 45, "original": 2400.00, "price": 1799.00, "strokes": [2], "tags": ["儿童", "启蒙"], "desc": "儿童蛙泳启蒙，培养水感与基础动作。"},
    {"name": "全泳姿全能班20节", "mode": "standard", "teaching": "one_on_three", "hours": 20, "duration": 90, "days": 120, "original": 10000.00, "price": 7999.00, "strokes": [1, 2, 3, 4], "tags": ["全能", "长训"], "desc": "四种泳姿系统学习，适合长期训练。"},
]

# 中文泳姿映射（用于用户和教练的 swim_strokes / teaching_strokes 展示）
STROKE_OPTIONS = {
    1: "自由泳",
    2: "蛙泳",
    3: "仰泳",
    4: "蝶泳",
}

# 英文键，用于 package_template.stroke_ids 等 JSON 字段
STROKE_IDS = {
    1: "freestyle",
    2: "breaststroke",
    3: "backstroke",
    4: "butterfly",
}


def sql_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "1" if v else "0"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (list, dict)):
        import json
        return "'" + json.dumps(v, ensure_ascii=False).replace("'", "''") + "'"
    return "'" + str(v).replace("'", "''") + "'"


def main():
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    upload_images = _load_upload_images()
    if len(upload_images) < COACH_COUNT + USER_COUNT:
        print(f"警告: uploads/ 下只有 {len(upload_images)} 张图片，建议至少 {COACH_COUNT + USER_COUNT} 张")

    lines = []
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    lines.append("-- AI 助理前置种子数据")
    lines.append("-- 生成时间: " + now)
    lines.append("-- 说明: 10 名教练 + 10 个套餐模板 + 20 名用户，每名用户至少 1 条 active 套餐")
    lines.append("")
    lines.append("SET FOREIGN_KEY_CHECKS = 0;")
    lines.append("SET NAMES utf8mb4;")
    lines.append("")
    lines.append("-- 清理已存在的种子数据，确保脚本可重复执行")
    lines.append("DELETE FROM `package` WHERE package_no LIKE 'P-SEED-%';")
    lines.append("DELETE FROM `order` WHERE order_no LIKE 'O-SEED-%';")
    lines.append("DELETE FROM `user` WHERE openid LIKE 'seed_u_%';")
    tmpl_names = [spec["name"] for spec in TEMPLATE_SPECS]
    lines.append(
        "DELETE FROM package_template_coach WHERE package_template_id IN ("
        "SELECT id FROM package_template WHERE name IN (" + ", ".join(sql_val(n) for n in tmpl_names) + "));"
    )
    lines.append(
        "DELETE FROM package_template WHERE name IN (" + ", ".join(sql_val(n) for n in tmpl_names) + ");"
    )
    lines.append("DELETE FROM coach WHERE openid LIKE 'seed_c_%';")
    lines.append("")
    lines.append("SET @coach_base = (SELECT COALESCE(MAX(id),0)+10000 FROM coach);")
    lines.append("SET @tmpl_base  = (SELECT COALESCE(MAX(id),0)+10000 FROM package_template);")
    lines.append("SET @user_base  = (SELECT COALESCE(MAX(id),0)+10000 FROM `user`);")
    lines.append("SET @order_base = (SELECT COALESCE(MAX(id),0)+10000 FROM `order`);")
    lines.append("SET @pkg_base   = (SELECT COALESCE(MAX(id),0)+10000 FROM `package`);")
    lines.append("")

    # coaches
    coach_rows = []
    for i in range(COACH_COUNT):
        cid = f"(@coach_base + {i+1})"
        idx = i + 1
        gender = "male" if i % 2 == 0 else "female"
        # 擅长泳姿：中文，逗号分隔
        strokes_cn = ",".join([STROKE_OPTIONS[s] for s in TEMPLATE_SPECS[i]["strokes"]])
        # 手机号加密
        coach_phone = _make_coach_phone(i)
        encrypted_phone = _encrypt_phone(coach_phone)
        phone_hash = _hash_phone(coach_phone)
        # 身份证号
        id_card = _make_id_card(i, gender)
        # 邮箱
        pinyin = ["li", "wang", "zhang", "liu", "chen", "yang", "huang", "zhao", "zhou", "wu"][i]
        email = f"{pinyin}.coach{idx}@example.com"
        # 总学员数 / 总课时数
        total_students = 30 + i * 12
        total_hours = 120 + i * 35
        # 教练 certificates 使用空数组，避免 admin 列表因 DTO 类型映射报错
        coach_rows.append(
            f"({cid}, 'seed_c_{idx:04d}_openid', 'seed_c_{idx:04d}_union', "
            f"{sql_val(encrypted_phone)}, {sql_val(phone_hash)}, "
            f"{sql_val(_avatar_url(upload_images, i))}, "
            f"{sql_val(COACH_NAMES[i])}, 1, {25 + (i % 15)}, {sql_val(gender)}, "
            f"1, {3 + (i % 12)}, {sql_val(strokes_cn)}, {sql_val(COACH_BIOS[i])}, "
            f"{sql_val(email)}, {sql_val(id_card)}, {sql_val(hashlib.sha256(id_card.encode()).hexdigest())}, "
            f"{total_students}, {total_hours}, "
            f"{200 + i*20:.2f}, {4.5 + (i % 5)*0.1:.1f}, "
            f"'[]', "
            f"NOW(), NOW())"
        )
    lines.append("INSERT INTO coach (id, openid, union_id, phone, phone_hash, avatar_url, name, status, age, gender, profile_completed, teaching_years, teaching_strokes, bio, email, id_card_no, id_card_hash, total_students, total_hours, reference_price, rating, certificates, created_at, updated_at)")
    lines.append("VALUES " + ",\n".join(coach_rows) + ";")
    lines.append("")

    # package templates
    tmpl_rows = []
    for i in range(TEMPLATE_COUNT):
        tid = f"(@tmpl_base + {i+1})"
        spec = TEMPLATE_SPECS[i]
        tmpl_rows.append(
            f"({tid}, {sql_val(spec['name'])}, {sql_val(spec['mode'])}, {sql_val(spec['teaching'])}, "
            f"'{json.dumps(spec['strokes'])}', {spec['hours']}, {spec['duration']}, {spec['days']}, "
            f"{spec['original']:.2f}, {spec['price']:.2f}, 1, 0.80, 7, "
            f"'{json.dumps(spec['tags'], ensure_ascii=False)}', {sql_val(spec['desc'])}, "
            f"'[{json.dumps('https://example.com/img/tmpl'+str(i+1)+'.png')}]', "
            f"'active', NOW(), NOW(), 0)"
        )
    lines.append("INSERT INTO package_template (id, name, package_mode, teaching_type, stroke_ids, total_hours, duration_minutes, valid_days, original_price, price, refund_enabled, refund_ratio, refund_valid_days, tags, description, images, status, created_at, updated_at, version)")
    lines.append("VALUES " + ",\n".join(tmpl_rows) + ";")
    lines.append("")

    # package_template_coach: each template linked to its corresponding coach
    ptc_rows = []
    for i in range(TEMPLATE_COUNT):
        ptc_rows.append(
            f"((@tmpl_base + {i+1}), (@coach_base + {i+1}), {200 + i*20:.2f}, NOW(), NOW())"
        )
    lines.append("INSERT INTO package_template_coach (package_template_id, coach_id, reference_price_snapshot, created_at, updated_at)")
    lines.append("VALUES " + ",\n".join(ptc_rows) + ";")
    lines.append("")

    # users
    user_rows = []
    for i in range(USER_COUNT):
        uid = f"(@user_base + {i+1})"
        idx = i + 1
        gender = "male" if i % 3 != 0 else "female"
        has_basis = 1 if i % 4 != 0 else 0
        # 擅长泳姿：中文数组（所有用户都填写，便于 AI 推荐时参考）
        strokes_cn = ["蛙泳", "自由泳"] if i % 2 == 0 else ["自由泳"]
        swim_years = (i % 5) + 1 if has_basis else "NULL"
        phone = _make_user_phone(i)
        encrypted_phone = _encrypt_phone(phone)
        phone_hash = _hash_phone(phone)
        user_rows.append(
            f"({uid}, 'seed_u_{idx:04d}_openid', 'seed_u_{idx:04d}_union', "
            f"{sql_val(encrypted_phone)}, {sql_val(phone_hash)}, "
            f"{sql_val(_avatar_url(upload_images, COACH_COUNT + i))}, {sql_val(USER_NAMES[i])}, "
            f"'注册用户', 1, 0, NOW(), NOW(), {18 + (i % 40)}, {sql_val(gender)}, "
            f"{has_basis}, {sql_val(strokes_cn)}, {swim_years}, {sql_val('想系统学习游泳')})"
        )
    lines.append("INSERT INTO `user` (id, openid, union_id, phone, phone_hash, avatar_url, name, identity_status, profile_completed, status, created_at, updated_at, age, gender, has_swim_basis, swim_strokes, swim_years, personal_desc)")
    lines.append("VALUES " + ",\n".join(user_rows) + ";")
    lines.append("")

    # packages (insert before orders so that orders can reference package_id)
    pkg_rows = []
    for i in range(USER_COUNT):
        pid = f"(@pkg_base + {i+1})"
        idx = i + 1
        coach_offset = (i % COACH_COUNT) + 1
        tmpl_offset = (i % TEMPLATE_COUNT) + 1
        spec = TEMPLATE_SPECS[i % TEMPLATE_COUNT]
        price_per_hour = round(spec["price"] / spec["hours"], 2)
        stroke_json = json.dumps(spec["strokes"])
        pkg_rows.append(
            f"({pid}, {sql_val(f'P-SEED-{idx:04d}')}, (@user_base + {idx}), (@coach_base + {coach_offset}), "
            f"(@order_base + {idx}), {sql_val(spec['mode'])}, {spec['hours']}, 0, 0, {spec['hours']}, "
            f"{price_per_hour:.2f}, {spec['price']:.2f}, {spec['original']:.2f}, 1, 0.80, 7, 'active', "
            f"NULL, {sql_val(spec['name'])}, {sql_val(COACH_NAMES[coach_offset-1])}, {sql_val(spec['teaching'])}, "
            f"'{stroke_json}', {spec['duration']}, {spec['days']}, NULL, "
            f"DATE_ADD(NOW(), INTERVAL {spec['days']} DAY), NULL, NULL, 0, NOW(), NOW())"
        )
    lines.append("INSERT INTO `package` (id, package_no, user_id, coach_id, order_id, package_mode, total_hours, consumed_count, reserved_count, available_count, price_per_hour, paid_amount, original_price, refund_enabled, refund_ratio, refund_valid_days, status, frozen_reason, package_name, coach_name, teaching_type, stroke_ids, duration_minutes, valid_days, pending_handover_at, expire_at, exhausted_at, refunded_at, version, created_at, updated_at)")
    lines.append("VALUES " + ",\n".join(pkg_rows) + ";")
    lines.append("")

    # orders
    order_rows = []
    for i in range(USER_COUNT):
        oid = f"(@order_base + {i+1})"
        idx = i + 1
        coach_offset = (i % COACH_COUNT) + 1
        tmpl_offset = (i % TEMPLATE_COUNT) + 1
        spec = TEMPLATE_SPECS[i % TEMPLATE_COUNT]
        stroke_json = json.dumps(spec["strokes"])
        order_rows.append(
            f"({oid}, {sql_val(f'O-SEED-{idx:04d}')}, 'purchase', 'paid', "
            f"(@user_base + {idx}), (@coach_base + {coach_offset}), (@pkg_base + {idx}), NULL, (@tmpl_base + {tmpl_offset}), "
            f"{spec['original']:.2f}, 0.00, {spec['price']:.2f}, 'wechat', "
            f"{sql_val(f'MOCK-{idx:08d}')}, NULL, NULL, NULL, NULL, NULL, NULL, NULL, "
            f"{sql_val(spec['mode'])}, {sql_val(spec['name'])}, {sql_val(COACH_NAMES[coach_offset-1])}, "
            f"{sql_val(spec['teaching'])}, '{stroke_json}', {spec['hours']}, {spec['duration']}, {spec['days']}, "
            f"1, 0.80, 7, NOW(), NOW(), NOW())"
        )
    lines.append("INSERT INTO `order` (id, order_no, type, status, user_id, coach_id, package_id, purchase_order_id, package_template_id, original_amount, discount_amount, paid_amount, payment_method, channel_trade_no, reason, rejected_reason, adjust_reason, approved_by, approved_at, refunded_at, expire_at, package_mode, package_name, coach_name, teaching_type, stroke_ids, total_hours, duration_minutes, valid_days, refund_enabled, refund_ratio, refund_valid_days, paid_at, created_at, updated_at)")
    lines.append("VALUES " + ",\n".join(order_rows) + ";")
    lines.append("")

    lines.append("SET FOREIGN_KEY_CHECKS = 1;")
    lines.append("")

    with open(OUTPUT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"已生成: {OUTPUT}")


if __name__ == "__main__":
    main()
