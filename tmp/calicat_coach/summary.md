# Calicat Coach First Batch Design Summary

## C-coach-center-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-         [图层]: 我的  (fontSize=17px, fill=#262626, w=35)
-           [图层]: 张  (fontSize=24px, fill=#1890FF, w=25)
-               [图层]: 张明远  (fontSize=20px, fill=#262626, w=fill_container)
-                 [图层]: 在职  (fontSize=12px, fill=#52C41A, w=25)
-             [图层]:   (fontSize=20px, w=22)
-           [图层]:   (fontSize=18px, w=20)
-               [图层]: 资料审核中  (fontSize=14px, fill=#262626, w=fill_container)
-           [图层]: 审核通常需 1-3 个工作日  (fontSize=12px, fill=#262626, w=fill_container)
-           [图层]:   (fontSize=22px, w=24)
-               [图层]: 个人主页编辑  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]:   (fontSize=22px, w=24)
-               [图层]: 参考单价设置  (fontSize=16px, fill=#262626, w=fill_container)
-               [图层]: ¥200/节  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]:   (fontSize=22px, w=24)
-               [图层]: 我的收入  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]:   (fontSize=22px, w=24)
-               [图层]: 排班管理  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]:   (fontSize=22px, w=24)
-               [图层]: 请假申请  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]:   (fontSize=22px, w=24)
-               [图层]: 申请离职  (fontSize=16px, fill=#FF4D4F, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-             [图层]:   (fontSize=24px, w=fit_content)
-               [图层]: 首页  (fontSize=10px, fill=#8C8C8C, w=fit_content)
-             [图层]:   (fontSize=24px, w=fit_content)
-               [图层]: 预约  (fontSize=10px, fill=#8C8C8C, w=fit_content)
-             [图层]:   (fontSize=24px, w=fit_content)
-               [图层]: 学员  (fontSize=10px, fill=#8C8C8C, w=fit_content)
-             [图层]:   (fontSize=24px, w=fit_content)
-               [图层]: 我的  (fontSize=10px, fill=#1890FF, w=fit_content)

### 主要 Frame 容器

- C-教练中心页 3 375xfit_content corner=None padding=None
-   页面容器 fill_containerxfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       标题区 fill_containerxfit_content corner=None padding=None
-     内容区 fill_containerxfit_content corner=None padding=[16, 16, 96, 16]
-       教练资料头图卡片 fill_containerx120 corner=12 padding=16 bg="rgba(255,255,255,1)"
-         头像 64x64 corner=32 padding=None bg="rgba(230,247,255,1)"
-         container fit_contentxfit_content corner=None padding=[0, 0, 0, 16]
-           信息区 fit_contentxfit_content corner=None padding=None
-             姓名 60xfit_content corner=None padding=None
-             状态标签 40xfit_content corner=None padding=[8, 0, 0, 0]
-               标签 fill_containerx22 corner=11 padding=[0, 8, 0, 8] bg="rgba(246,255,237,1)"
-         container fit_contentxfit_content corner=None padding=[0, 0, 0, 135.16250610351562]
-           箭头 fit_contentxfit_content corner=None padding=None
-       状态提示卡 fill_containerxfit_content corner=12 padding=16 bg="rgba(230,247,255,1)"
-         标题行 fill_containerxfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 8]
-             标题 70xfit_content corner=None padding=None
-         正文 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-       功能入口列表卡片 fill_containerxfit_content corner=12 padding=None bg="rgba(255,255,255,1)"
-         入口-个人主页编辑 fill_containerx56 corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12.000001907348633]
-             标题 96xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 146.3249969482422]
-         入口-参考单价设置 fill_containerx56 corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12.000001907348633]
-             标题 96xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 87.75]
-             当前值 51xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 7.999996185302734]
-         入口-我的收入 fill_containerx56 corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12.000001907348633]
-             标题 64xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 178.3249969482422]
-         入口-排班管理 fill_containerx56 corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12.000001907348633]
-             标题 64xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 178.3249969482422]
-         入口-请假申请 fill_containerx56 corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12.000001907348633]
-             标题 64xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 178.3249969482422]
-         入口-申请离职 fill_containerx56 corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12.000001907348633]
-             标题 64xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 178.3249969482422]
-     TabBar 2 fill_containerx83 corner=None padding=None bg="rgba(255,255,255,1)"
-       TabBar内容 fill_containerx49 corner=None padding=[8, 32, 0, 32]
-         Tab项容器 fill_containerx52 corner=None padding=None
-           首页Tab fit_contentxfill_container corner=None padding=None
-             container fit_contentxfit_content corner=None padding=[2, 0, 0, 0]
-           预约Tab fit_contentxfill_container corner=None padding=None
-             container fit_contentxfit_content corner=None padding=[2, 0, 0, 0]
-           学员Tab fit_contentxfill_container corner=None padding=None
-             container fit_contentxfit_content corner=None padding=[2, 0, 0, 0]
-           我的Tab fit_contentxfill_container corner=None padding=None
-             container fit_contentxfit_content corner=None padding=[2, 0, 0, 0]

### SVG/图标


---

## C-coach-onboarding-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 背景: "rgba(245,247,250,1)"
- 子节点数: 1

### 文本内容

-       [图层]: 入驻资料  (fontSize=17px, fill=#262626, w=fit_content)
-         [图层]: 审核未通过  (fontSize=14px, fill=#FF4D4F, w=fill_container)
-         [图层]: 请上传清晰的教练资格证书，照片需清晰可见  (fontSize=14px, fill=#262626, w=fill_container)
-         [关闭图标]:   (fontSize=16px, w=fit_content)
-         [分组标题]: 实名与资质  (fontSize=16px, fill=#262626, w=fit_content)
-           [图层]: 身份证号  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 请输入18位身份证号  (fontSize=14px, fill=#BFBFBF, w=fit_content)
-           [图层]: 身份证正面照  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]:   (fontSize=24px, w=fit_content)
-           [图层]: 身份证反面照  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]:   (fontSize=24px, w=fit_content)
-           [图层]: 教练资格证  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]:   (fontSize=24px, w=fit_content)
-                 [图层]:   (fontSize=24px, w=fit_content)
-               [图层]: 至少1张，最多9张，单张≤5MB，JPG/PNG  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-           [图层]: 健康证  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]:   (fontSize=24px, w=fit_content)
-           [图层]: 个人形象照  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]:   (fontSize=24px, w=fit_content)
-         [分组标题]: 服务设置  (fontSize=16px, fill=#262626, w=fit_content)
-             [图层]: 参考单价  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 50-2000  (fontSize=14px, fill=#BFBFBF, w=fit_content)
-                   [图层]: 元/节  (fontSize=14px, fill=#8C8C8C, w=35)
-           [图层]: 参考单价范围 50-2000 元/节  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-         [分组标题]: 基础信息  (fontSize=16px, fill=#262626, w=fit_content)
-             [图层]: 姓名/昵称  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 请输入姓名  (fontSize=14px, fill=#BFBFBF, w=fit_content)
-             [图层]: 手机号  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 138****6789  (fontSize=14px, fill=#8C8C8C, w=fit_content)
-         [分组标题]: 教学履历  (fontSize=16px, fill=#262626, w=fit_content)
-             [图层]: 任教年限  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 请输入任教年限  (fontSize=14px, fill=#BFBFBF, w=99)
-                   [图层]: 年  (fontSize=14px, fill=#8C8C8C, w=15)
-             [图层]: 总学员数  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 请输入总学员数  (fontSize=14px, fill=#BFBFBF, w=99)
-                   [图层]: 人  (fontSize=14px, fill=#8C8C8C, w=15)
-             [图层]: 总课时数  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 请输入总课时数  (fontSize=14px, fill=#BFBFBF, w=99)
-                   [图层]: 节  (fontSize=14px, fill=#8C8C8C, w=15)
-           [图层]: 擅长泳姿  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 请选择（可选）  (fontSize=14px, fill=#BFBFBF, w=fit_content)
-             [图层]: 个人简介  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 请介绍您的教学经历和特长  (fontSize=14px, fill=#BFBFBF, w=169)
-               [图层]: 0/500  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 保存草稿  (fontSize=16px, fill=#1890FF, w=65)
-               [图层]: 提交审核  (fontSize=16px, fill=#FFFFFF, w=fit_content)

### 主要 Frame 容器

- C-入驻资料填写页 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-   页面容器 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=None bg="rgba(255,255,255,1)"
-     驳回提示容器 fill_containerxfit_content corner=None padding=[12, 16, 0, 16]
-       驳回原因提示条 fill_containerxfit_content corner=12 padding=12 bg="rgba(255,241,240,1)"
-     内容区 fill_containerxfit_content corner=None padding=[16, 16, 40, 16]
-       表单区 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             上传格 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             上传格 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             上传区 fill_containerxfit_content corner=None padding=None
-               上传格 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-               上传格 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-             container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             上传格 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             上传格 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-       表单区 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           总学员数 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-                 container fit_contentxfit_content corner=None padding=[0, 0, 0, 173.66667556762695]
-         container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-       表单区 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           任教年限 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           手机号 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(245,245,245,1)"
-       表单区 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           任教年限 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-                 container fit_contentxfit_content corner=None padding=[0, 0, 0, 173.66667556762695]
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           总学员数 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-                 container fit_contentxfit_content corner=None padding=[0, 0, 0, 173.66667556762695]
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           总课时数 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-                 container fit_contentxfit_content corner=None padding=[0, 0, 0, 173.66667556762695]
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             标签组 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           个人简介 fill_containerxfit_content corner=None padding=None
-             container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-               文本域 fill_containerx120 corner=8 padding=[12, 12, 0, 12] bg="rgba(255,255,255,1)"
-             container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[16, 16, 32, 16] bg="rgba(255,255,255,1)"
-         底部操作区 fill_containerxfit_content corner=None padding=[16, 16, 32, 16] bg="rgba(255,255,255,1)"
-           保存草稿 fill_containerx44 corner=8 padding=None bg="rgba(255,255,255,1)"
-           container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-             提交审核 fill_containerx48 corner=8 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 1.1102230246251565e-16, "x2": 1, "y2

### SVG/图标


---

## C-coach-onboarding-success-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-         [图层]: 提交成功  (fontSize=24px, fill=#262626, w=fill_container)
-         [图层]: 提交成功，等待审核  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-         [图层]: 2 秒后自动跳转等待审核页  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 已提交资料  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 审核中  (fontSize=12px, fill=#FAAD14, w=37)
-             [图层]: 张  (fontSize=20px, fill=#1890FF, w=21)
-                 [图层]: 张明远  (fontSize=16px, fill=#262626, w=fill_container)
-                 [图层]: 138****6789  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]: 任教年限  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 8年  (fontSize=14px, fill=#262626, w=fill_container)
-             [图层]: 参考单价  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 300.00 元/节  (fontSize=14px, fill=#262626, w=fill_container)
-           [图层]: 审核结果将通过服务通知推送  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-         [图层]: 查看审核进度  (fontSize=16px, fill=#FFFFFF, w=97)

### 主要 Frame 容器

- C-入驻提交成功页 3 375xfit_content corner=None padding=None
-   页面容器 fill_containerxfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     成功插画区 fill_containerxfit_content corner=None padding=[48, 0, 0, 0]
-       成功插画 120x120 corner=None padding=None
-         审核通过插画 fit_contentxfit_content corner=None padding=None
-       主标题区 96xfit_content corner=None padding=[20, 0, 0, 0]
-       副标题区 126xfit_content corner=None padding=[8, 0, 0, 0]
-       自动跳转提示 142xfit_content corner=None padding=[4, 0, 0, 0]
-     资料摘要卡容器 fill_containerxfit_content corner=None padding=[32, 16, 0, 16]
-       卡片容器 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         卡片头部 fill_containerxfit_content corner=None padding=None
-           卡片标题 70xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 185.20001220703125]
-             审核标签 56xfit_content corner=None padding=None
-               标签 fill_containerx24 corner=12 padding=[0, 10, 0, 10] bg="rgba(255,251,230,1)"
-         教练信息行 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           头像 48x48 corner=24 padding=None bg="rgba(230,247,255,1)"
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12]
-             姓名手机号 fit_contentxfit_content corner=None padding=None
-               姓名 48xfit_content corner=None padding=None
-               手机号 81xfit_content corner=None padding=None
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         任教年限行 fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-           标签 56xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 233.42501831054688]
-             值 22xfit_content corner=None padding=None
-         参考单价行 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-           标签 56xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 175.82501220703125]
-             值 80xfit_content corner=None padding=None
-         底部提示 fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-     操作按钮区 fill_containerxfit_content corner=None padding=[20, 16, 32, 16]
-       查看审核进度 fill_containerx48 corner=8 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 0.5, "x2": 1, "y2": 0.5}, "colorStop

### SVG/图标

-           矢量 104x104 fills=#E6F7FF stroke={'align': 'center', 'thickness': 3, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#1890FF'}
-           矢量 44x30 fills=None stroke={'align': 'center', 'thickness': 4, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#1890FF'}
-           矢量 70x10.31 fills=None stroke={'align': 'center', 'thickness': 2, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#1890FF'}

---

## C-coach-pending-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-       [图层]: 等待审核  (fontSize=17px, fill=#262626, w=69)
-         [图层]: 审核中，请耐心等待  (fontSize=20px, fill=#262626, w=fill_container)
-         [图层]: 预计 1-3 个工作日内完成审核  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]: 已提交资料  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 审核中  (fontSize=12px, fill=#FAAD14, w=37)
-             [图层]: 张  (fontSize=20px, fill=#1890FF, w=21)
-                 [图层]: 张明远  (fontSize=16px, fill=#262626, w=fill_container)
-                 [图层]: 138****6789  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]: 参考单价  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 300.00 元/节  (fontSize=14px, fill=#262626, w=fill_container)
-             [图层]: 提交时间  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-               [图层]: 2026-08-05 14:30  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-         [图层]: 查看完整入驻资料  (fontSize=16px, fill=#1890FF, w=129)
-         [图层]: 联系客服 / 帮助  (fontSize=14px, fill=#1890FF, w=fill_container)

### 主要 Frame 容器

- C-等待审核页 4 375xfit_content corner=None padding=None
-   页面容器 fill_containerxfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=None bg="rgba(255,255,255,1)"
-     审核状态区 fill_containerxfit_content corner=None padding=[48, 0, 0, 0]
-       状态图标 80x80 corner=40 padding=None bg="rgba(255,251,230,1)"
-         时钟图标 fit_contentxfit_content corner=None padding=None
-       主标题区 180xfit_content corner=None padding=[16, 0, 0, 0]
-       副标题区 181xfit_content corner=None padding=[8, 0, 0, 0]
-     已提交资料摘要卡 fill_containerxfit_content corner=None padding=[24, 16, 0, 16]
-       卡片容器 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         卡片头部 fill_containerxfit_content corner=None padding=None
-           卡片标题 70xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 185.20001220703125]
-             审核标签 56xfit_content corner=None padding=None
-               标签 fill_containerx24 corner=12 padding=[0, 10, 0, 10] bg="rgba(255,251,230,1)"
-         教练信息行 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           头像 48x48 corner=24 padding=None bg="rgba(230,247,255,1)"
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 12]
-             姓名手机号 fit_contentxfit_content corner=None padding=None
-               姓名 48xfit_content corner=None padding=None
-               手机号 81xfit_content corner=None padding=None
-         container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         参考单价行 fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-           标签 56xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 175.82501220703125]
-             值 80xfit_content corner=None padding=None
-         提交时间行 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-           标签 48xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 168.9250030517578]
-             值 95xfit_content corner=None padding=None
-     操作区 fill_containerxfit_content corner=None padding=[20, 16, 0, 16]
-       查看完整入驻资料 fill_containerx48 corner=8 padding=None bg="rgba(255,255,255,1)"
-       联系客服入口 96xfit_content corner=None padding=[16, 0, 0, 0]

### SVG/图标

-           矢量 32x32 fills=None stroke={'align': 'center', 'thickness': 2.5, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#FAAD14'}
-           矢量 6x12 fills=None stroke={'align': 'center', 'thickness': 2.5, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#FAAD14'}

---

## C-coach-resigning-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-         [图层]:   (fontSize=22px, w=24)
-         [图层]: 离职处理  (fontSize=17px, fill=#262626, w=69)
-         [图层]: 离职申请已提交，正在处理中  (fontSize=20px, fill=#262626, w=fill_container)
-         [图层]: 请耐心等待管理员审批  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]: 工单进度  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 处理中  (fontSize=12px, fill=#FAAD14, w=37)
-           [图层]: 工单号：RESIGN-20260805-001  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 当前进度  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 已提交管理员审批  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 更新时间  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-               [图层]: 2026-08-05 14:30  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 预计处理  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-               [图层]: 预计 1-3 个工作日内完成审批  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-           [图层]: 温馨提示  (fontSize=14px, fill=#262626, w=fill_container)
-           [图层]: 审批期间你可继续查看历史学员资料；审批通过后账号将转为已离职状态，如需恢复教学资格请重新提交入驻资料。  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-         [图层]: 查看离职工单  (fontSize=16px, fill=#FFFFFF, w=97)
-         [图层]: 联系客服 / 帮助  (fontSize=14px, fill=#1890FF, w=fill_container)

### 主要 Frame 容器

- C-离职处理中页 4 375xfit_content corner=None padding=None
-   页面容器 fill_containerxfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       返回按钮 32x32 corner=None padding=None
-       标题区 fill_containerxfit_content corner=None padding=None
-     状态图标区 fill_containerxfit_content corner=None padding=[48, 0, 0, 0]
-       状态图标 80x80 corner=40 padding=None bg="rgba(255,251,230,1)"
-         时钟图标 fit_contentxfit_content corner=None padding=None
-       主标题区 260xfit_content corner=None padding=[16, 0, 0, 0]
-       副标题区 140xfit_content corner=None padding=[8, 0, 0, 0]
-     工单进度卡 fill_containerxfit_content corner=None padding=[24, 16, 0, 16]
-       卡片容器 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         卡片头部 fill_containerxfit_content corner=None padding=None
-           卡片标题 56xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 199.20001220703125]
-             状态标签 56xfit_content corner=None padding=None
-               标签 fill_containerx24 corner=12 padding=[0, 10, 0, 10] bg="rgba(255,251,230,1)"
-         工单号行 fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-         container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-         当前进度行 fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-           进度标签 56xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 127.19999694824219]
-             进度值 128xfit_content corner=None padding=None
-         进度时间行 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-           时间标签 48xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 168.9250030517578]
-             时间值 95xfit_content corner=None padding=None
-         预计处理时间行 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-           预计标签 48xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 108.33750915527344]
-             预计值 155xfit_content corner=None padding=None
-     温馨提示区 fill_containerxfit_content corner=None padding=[16, 16, 0, 16]
-       提示容器 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         提示标题 fill_containerxfit_content corner=None padding=None
-         提示内容 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-     操作区 fill_containerxfit_content corner=None padding=[20, 16, 32, 16]
-       查看离职工单 fill_containerx48 corner=8 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 0.5, "x2": 1, "y2": 0.5}, "colorStop
-       联系客服入口 96xfit_content corner=None padding=[16, 0, 0, 0]

### SVG/图标

-           矢量 32x32 fills=None stroke={'align': 'center', 'thickness': 2.5, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#FAAD14'}
-           矢量 6x12 fills=None stroke={'align': 'center', 'thickness': 2.5, 'lineCap': 'round', 'lineJoin': 'round', 'fills': '#FAAD14'}

---

## C-phone-login-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-           [图层]: leyoSwimming  (fontSize=22px, fill=#FFFFFF, w=fill_container)
-           [图层]: 专业游泳约课平台  (fontSize=13px, fill=#FFFFFFD8, w=fill_container)
-         [图层]: 手机号登录  (fontSize=22px, fill=#262626, w=fill_container)
-             [图层]: 输入手机号获取验证码，即可快速登录  (fontSize=13px, fill=#8C8C8C, w=fill_container)
-             [图层]: 手机号  (fontSize=14px, fill=#262626, w=43)
-                 [图层]:   (fontSize=18px, w=20)
-                     [图层]: 请输入11位手机号  (fontSize=14px, fill=#BFBFBF, w=fill_container)
-             [图层]: 验证码  (fontSize=14px, fill=#262626, w=43)
-                 [图层]:   (fontSize=18px, w=20)
-                     [图层]: 请输入短信验证码  (fontSize=14px, fill=#BFBFBF, w=fill_container)
-                 [图层]: 获取验证码  (fontSize=13px, fill=#FFFFFF, w=66)
-               [图层]: 已阅读并同意  (fontSize=12px, fill=#8C8C8C, w=73)
-               [图层]: 《隐私协议》  (fontSize=12px, fill=#1890FF, w=73)
-               [图层]: 《用户须知》  (fontSize=12px, fill=#1890FF, w=73)
-             [图层]: 登录  (fontSize=16px, fill=#FFFFFF, w=33)
-               [图层]: 其他登录方式  (fontSize=12px, fill=#BFBFBF, w=fill_container)
-               [图层]:   (fontSize=20px, w=22)
-                   [图层]: 使用微信登录  (fontSize=14px, fill=#595959, w=fill_container)
-               [图层]: 未注册手机号将自动创建账号  (fontSize=12px, fill=#BFBFBF, w=157)

### 主要 Frame 容器

- C-手机号登录页 375xfit_content corner=None padding=None
-   页面容器 375xfit_content corner=None padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0.49999999999999994, "y1": 0, "x2": 0.5, "y2"
-     品牌区 fill_containerxfit_content corner=None padding=[32, 0, 24, 0]
-       品牌Logo 80x80 corner=40 padding=None bg="rgba(255,255,255,0.15)"
-         Logo图标 fit_contentxfit_content corner=None padding=None
-       container fit_contentxfit_content corner=None padding=[16, 0, 0, 0]
-         品牌名称 162xfit_content corner=None padding=None
-       container fit_contentxfit_content corner=None padding=[6, 0, 0, 0]
-         品牌Slogan 104xfit_content corner=None padding=None
-     表单卡片区 fill_containerxfit_content corner=[20, 20, 0, 0] padding=[28, 20, 0, 20] bg="rgba(255,255,255,1)"
-       页面标题区 fill_containerxfit_content corner=None padding=None
-         container fill_containerxfit_content corner=None padding=[6, 0, 0, 0]
-           副标题 fill_containerxfit_content corner=None padding=None
-       container fill_containerxfit_content corner=None padding=[24, 0, 0, 0]
-         手机号输入区 fill_containerxfit_content corner=None padding=None
-           标签行 fill_containerxfit_content corner=None padding=None
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框行 fill_containerxfit_content corner=None padding=None
-               手机号输入框 fill_containerx46 corner=10 padding=[0, 14, 0, 14] bg="rgba(245,247,250,1)"
-                 container fit_contentxfit_content corner=None padding=[0, 0, 0, 8]
-                   placeholder 114xfit_content corner=None padding=None
-       container fill_containerxfit_content corner=None padding=[18, 0, 0, 0]
-         验证码输入区 fill_containerxfit_content corner=None padding=None
-           标签行 fill_containerxfit_content corner=None padding=None
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框行 fill_containerxfit_content corner=None padding=None
-               验证码输入框 fill_containerx46 corner=10 padding=[0, 14, 0, 14] bg="rgba(245,247,250,1)"
-                 container fit_contentxfit_content corner=None padding=[0, 0, 0, 8]
-                   placeholder 112xfit_content corner=None padding=None
-               获取验证码按钮 100x46 corner=10 padding=None bg="rgba(24,144,255,1)"
-       container fill_containerxfit_content corner=None padding=[20, 0, 0, 0]
-         隐私协议勾选区 fill_containerxfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 8]
-             协议文案 fit_contentxfit_content corner=None padding=None
-       container fill_containerxfit_content corner=None padding=[24, 0, 0, 0]
-         登录按钮区 fill_containerxfit_content corner=None padding=None
-           登录按钮 fill_containerx48 corner=10 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 0.5, "x2": 1, "y2": 0.5}, "colorStop
-       container fill_containerxfit_content corner=None padding=[28, 0, 0, 0]
-         其他登录方式区 fill_containerxfit_content corner=None padding=[0, 0, 40, 0]
-           分割线区 fill_containerxfit_content corner=None padding=None
-             分割文字 104xfit_content corner=None padding=[0, 16, 0, 16]
-           container fit_contentxfit_content corner=None padding=[20, 0, 0, 0]
-             微信登录入口 fit_contentxfit_content corner=10 padding=[12, 24, 12, 24] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 7.999994277954102]
-                 文字 84xfit_content corner=None padding=None
-           container fit_contentxfit_content corner=None padding=[16.000038146972656, 0, 0, 0]
-             底部提示 fit_contentxfit_content corner=None padding=None

### SVG/图标

-           矢量 44x44 fills=None stroke={'align': 'center', 'thickness': 1.2, 'lineCap': 'butt', 'lineJoin': 'miter', 'fills': 'rgba(255,255,255,0.3)'}
-           矢量 20x30 fills=None stroke={'align': 'center', 'thickness': 2.5, 'lineCap': 'round', 'lineJoin': 'miter', 'fills': '#FFFFFF'}
-           矢量 6x6 fills=#FF6B35 stroke=None

---

## C-profile-edit-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-         [图层]:   (fontSize=22px, w=24)
-         [图层]: 编辑个人主页  (fontSize=17px, fill=#262626, w=103)
-           [图层]: 形象展示  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 个人形象照  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 张  (fontSize=28px, fill=#1890FF, w=29)
-                 [图层]:   (fontSize=24px, w=26)
-               [图层]: JPG/PNG，≤5MB，1张  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 姓名/昵称  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 张明远  (fontSize=14px, fill=#262626, w=43)
-             [图层]: 任教年限  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 8  (fontSize=14px, fill=#262626, w=9)
-                   [图层]: 年  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]: 实时状态标签  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 空闲中  (fontSize=12px, fill=#52C41A, w=37)
-                   [图层]: 系统自动更新  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-           [图层]: 基础信息  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 性别  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 男  (fontSize=14px, fill=#262626, w=15)
-               [图层]: 女  (fontSize=14px, fill=#262626, w=15)
-             [图层]: 年龄  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 32  (fontSize=14px, fill=#262626, w=17)
-                   [图层]: 岁  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-             [图层]: 邮箱  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: zhangmingyuan@example.com  (fontSize=14px, fill=#262626, w=207)
-             [图层]: 手机号  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 138****6789  (fontSize=14px, fill=#8C8C8C, w=82)
-                   [图层]: 微信授权  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 微信二维码  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 二维码  (fontSize=12px, fill=#1890FF, w=37)
-               [图层]: JPG/PNG，≤5MB，点击可预览/替换  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-           [图层]: 实名与资质  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 身份证号  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 320***********1234  (fontSize=14px, fill=#8C8C8C, w=128)
-             [图层]: 身份证正面照  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 身份证正面  (fontSize=12px, fill=#8C8C8C, w=61)
-             [图层]: 身份证反面照  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 身份证反面  (fontSize=12px, fill=#8C8C8C, w=61)
-             [图层]: 教练资格证  (fontSize=14px, fill=#262626, w=fill_container)
-                 [图层]: 资格证1  (fontSize=12px, fill=#8C8C8C, w=44)
-                 [图层]: 资格证2  (fontSize=12px, fill=#8C8C8C, w=44)
-             [图层]: 健康证  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 健康证  (fontSize=12px, fill=#8C8C8C, w=37)
-             [图层]:   (fontSize=16px, w=18)
-                 [图层]: 修改实名与资质需重新提交审核  (fontSize=13px, fill=#1890FF, w=fill_container)
-               [图层]:   (fontSize=18px, w=20)
-           [图层]: 教学履历  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 总学员数  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 326人（系统统计）  (fontSize=14px, fill=#8C8C8C, w=123)
-             [图层]: 总课时数  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 2,480节（系统统计）  (fontSize=14px, fill=#8C8C8C, w=134)
-             [图层]: 擅长泳姿  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 蛙泳  (fontSize=13px, fill=#1890FF, w=27)
-               [图层]: 自由泳  (fontSize=13px, fill=#1890FF, w=40)
-               [图层]: 仰泳  (fontSize=13px, fill=#8C8C8C, w=27)
-               [图层]: 蝶泳  (fontSize=13px, fill=#8C8C8C, w=27)
-             [图层]: 个人简介  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 国家一级游泳运动员，8年教学经验，擅长儿童及成人游泳教学，曾获全国游泳锦标赛金牌。  (fontSize=14px, fill=#262626, w=272)
-             [图层]: 45/500  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-           [图层]: 服务设置  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 参考单价  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: ¥200/节  (fontSize=14px, fill=#262626, w=fill_container)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]: 保存  (fontSize=16px, fill=#FFFFFF, w=33)

### 主要 Frame 容器

- C-个人主页编辑页 2 375xfit_content corner=None padding=None
-   页面容器 fill_containerxfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       返回按钮 32x32 corner=None padding=None
-       标题区 fill_containerxfit_content corner=None padding=None
-     内容区 fill_containerxfit_content corner=None padding=[16, 16, 40, 16]
-       形象展示卡片 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         分组标题 fill_containerxfit_content corner=None padding=None
-         个人形象照区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           上传区 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             头像预览+上传 fit_contentxfit_content corner=None padding=None
-               当前头像 80x80 corner=40 padding=None bg="rgba(230,247,255,1)"
-               替换上传格 80x80 corner=8 padding=None bg="rgba(245,247,250,1)"
-             提示文字 fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-         姓名区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           输入框容器 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-         任教年限区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           输入框容器 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 248.62501287460327]
-                 单位 14xfit_content corner=None padding=None
-         状态标签区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           只读框 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             只读框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(245,245,245,1)"
-               状态标签 fit_contentx22 corner=11 padding=[0, 8, 0, 8] bg="rgba(246,255,237,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 8.000003814697266]
-                 提示 72xfit_content corner=None padding=None
-       基础信息卡片 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         分组标题 fill_containerxfit_content corner=None padding=None
-         性别区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           单选组 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             选项-男 fit_contentxfit_content corner=None padding=None
-               单选-选中 20x20 corner=10 padding=None
-             选项-女 fit_contentxfit_content corner=None padding=None
-         年龄区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           输入框容器 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 240.85001277923584]
-                 单位 14xfit_content corner=None padding=None
-         邮箱区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           输入框容器 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-         手机号区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           只读框容器 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             只读框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(245,245,245,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 141.8500099182129]
-                 提示 48xfit_content corner=None padding=None
-         微信二维码区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           上传区 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             已上传预览 100x100 corner=8 padding=None bg="rgba(230,247,255,1)"
-             提示文字 fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-       实名与资质卡片 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         分组标题 fill_containerxfit_content corner=None padding=None
-         身份证号区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           只读框容器 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             只读框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(245,245,245,1)"
-         身份证正面照区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           预览区 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             预览图 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-         身份证反面照区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           预览区 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             预览图 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"
-         教练资格证区 fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-           标签 fill_containerxfit_content corner=None padding=None
-           预览区 fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             图片组 fill_containerxfit_content corner=None padding=None
-               预览图1 100x100 corner=8 padding=None bg="rgba(245,247,250,1)"

### SVG/图标


---

## C-reference-price-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 背景: "rgba(245,247,250,1)"
- 子节点数: 1

### 文本内容

-         [图层]:   (fontSize=22px, w=24)
-         [图层]: 参考单价  (fontSize=17px, fill=#262626, w=69)
-         [图层]: 参考单价说明  (fontSize=14px, fill=#0050B3, w=fill_container)
-           [图层]: 作为系统套餐定价和自定义套餐金额计算的基准。修改后不影响已购套餐。  (fontSize=12px, fill=#262626, w=fill_container)
-           [图层]: 每节课参考单价  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 200  (fontSize=14px, fill=#262626, w=25)
-                 [图层]: 元/节  (fontSize=14px, fill=#8C8C8C, w=35)
-             [图层]: 平台建议范围 50-2000 元/节  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-             [图层]: 今日还可修改2次  (fontSize=12px, fill=#FAAD14, w=fill_container)
-           [图层]: 基于当前单价的新套餐示例  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 1节体验课  (fontSize=14px, fill=#8C8C8C, w=65)
-               [图层]: ¥200  (fontSize=20px, fill=#262626, w=49)
-               [图层]: 6节标准课  (fontSize=14px, fill=#8C8C8C, w=65)
-               [图层]: ¥1,200  (fontSize=20px, fill=#262626, w=67)
-               [图层]: 10节标准课  (fontSize=14px, fill=#8C8C8C, w=73)
-               [图层]: ¥2,000  (fontSize=20px, fill=#262626, w=67)
-           [图层]: 保存  (fontSize=16px, fill=#FFFFFF, w=33)

### 主要 Frame 容器

- C-参考单价设置页 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-   页面容器 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       返回按钮 32x32 corner=None padding=None
-       标题 fill_containerx24 corner=None padding=None
-     内容区 fill_containerxfit_content corner=None padding=[16, 16, 40, 16]
-       说明卡片 fill_containerxfit_content corner=12 padding=16 bg="rgba(230,247,255,1)"
-         container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         价格输入区 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             输入框 fill_containerx44 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 228.86458206176758]
-           container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         套餐价格预览 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-           container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-             示例项 fill_containerx28 corner=None padding=None
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             示例项 fill_containerx28 corner=None padding=None
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             示例项 fill_containerx28 corner=None padding=None
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         保存按钮 fill_containerx48 corner=8 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 1.1102230246251565e-16, "x2": 1, "y2

### SVG/图标


---

## C-resignation-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 背景: "rgba(245,247,250,1)"
- 子节点数: 1

### 文本内容

-         [图层]:   (fontSize=22px, w=24)
-         [图层]: 申请离职  (fontSize=17px, fill=#262626, w=69)
-         [图层]: 离职影响说明  (fontSize=14px, fill=#FF4D4F, w=fill_container)
-           [图层]: 1. 提交后学员端不再展示您的购买入口  (fontSize=12px, fill=#262626, w=fill_container)
-         [图层]: 2. 需处理名下 active 学员套餐  (fontSize=12px, fill=#262626, w=fill_container)
-         [图层]: 3. 审批通过后教学资格将冻结  (fontSize=12px, fill=#262626, w=fill_container)
-           [图层]: 离职原因（选填）  (fontSize=14px, fill=#262626, w=fill_container)
-               [图层]: 请说明离职原因  (fontSize=14px, fill=#BFBFBF, w=99)
-             [图层]: 0/200  (fontSize=12px, fill=#8C8C8C, w=fill_container)
-           [图层]: 待处理学员套餐  (fontSize=14px, fill=#262626, w=99)
-             [图层]: 3份  (fontSize=14px, fill=#262626, w=23)
-             [图层]:   (fontSize=18px, w=20)
-           [图层]: 提交离职申请  (fontSize=16px, fill=#FFFFFF, w=97)

### 主要 Frame 容器

- C-离职申请页 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-   页面容器 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       返回按钮 32x32 corner=None padding=None
-       标题 fill_containerx24 corner=None padding=None
-     内容区 fill_containerxfit_content corner=None padding=[16, 16, 40, 16]
-       风险提示卡 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,241,240,1)"
-         container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         离职原因区 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             文本域 fill_containerx100 corner=8 padding=[12, 12, 0, 12] bg="rgba(255,255,255,1)"
-           container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         学员套餐处理入口 fill_containerx72 corner=12 padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 168.47918701171875]
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 3.999979019165039]
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         提交按钮 fill_containerx48 corner=8 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 1.1102230246251565e-16, "x2": 1, "y2

### SVG/图标


---

## C-resignation-ticket-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 背景: "rgba(245,247,250,1)"
- 子节点数: 1

### 文本内容

-         [图层]:   (fontSize=22px, w=24)
-         [图层]: 离职工单处理  (fontSize=17px, fill=#262626, w=103)
-               [图层]:   (fontSize=14px, w=16)
-               [图层]: 提交申请  (fontSize=12px, fill=#1890FF, w=fit_content)
-               [图层]: 2  (fontSize=12px, fill=#FFFFFF, w=8)
-               [图层]: 处理套餐  (fontSize=12px, fill=#1890FF, w=fit_content)
-               [图层]: 3  (fontSize=12px, fill=#BFBFBF, w=8)
-               [图层]: 提交工单  (fontSize=12px, fill=#BFBFBF, w=fit_content)
-               [图层]: 4  (fontSize=12px, fill=#BFBFBF, w=8)
-               [图层]: 审批结果  (fontSize=12px, fill=#BFBFBF, w=fit_content)
-         [图层]: 待处理学员套餐  (fontSize=16px, fill=#262626, w=fill_container)
-             [图层]: 刘先生  (fontSize=16px, fill=#262626, w=49)
-               [图层]: active  (fontSize=12px, fill=#52C41A, w=35)
-             [图层]: 10节正价课 · 剩余6课时  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 请选择处理方式  (fontSize=14px, fill=#BFBFBF, w=99)
-                 [图层]:   (fontSize=18px, w=20)
-             [图层]: 赵女士  (fontSize=16px, fill=#262626, w=49)
-               [图层]: active  (fontSize=12px, fill=#52C41A, w=35)
-             [图层]: 6节正价课 · 剩余1课时  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 全额退款  (fontSize=14px, fill=#262626, w=57)
-                 [图层]:   (fontSize=18px, w=20)
-             [图层]: 王小明  (fontSize=16px, fill=#262626, w=49)
-               [图层]: active  (fontSize=12px, fill=#52C41A, w=35)
-             [图层]: 10节正价课 · 剩余8课时  (fontSize=14px, fill=#8C8C8C, w=fill_container)
-               [图层]: 转新教练  (fontSize=14px, fill=#262626, w=57)
-                 [图层]:   (fontSize=18px, w=20)
-               [图层]:   (fontSize=16px, w=18)
-                 [图层]: 搜索新教练  (fontSize=14px, fill=#BFBFBF, w=71)
-         [图层]: 已处理2/3份套餐  (fontSize=14px, fill=#262626, w=fill_container)
-         [图层]: 提交审批  (fontSize=14px, fill=#FFFFFF, w=57)

### 主要 Frame 容器

- C-离职工单处理页 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-   页面容器 375xfit_content corner=None padding=None bg="rgba(245,247,250,1)"
-     NavBar fill_containerx44 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       返回按钮 32x32 corner=None padding=None
-       标题 fill_containerx24 corner=None padding=None
-     内容区 fill_containerxfit_content corner=None padding=[16, 16, 100, 16]
-       离职进度指示器 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-         步骤行 fill_containerxfit_content corner=None padding=None
-           步骤1-已完成 fit_contentxfit_content corner=None padding=None
-             圆点 24x24 corner=12 padding=None bg="rgba(24,144,255,1)"
-             container fit_contentxfit_content corner=None padding=[4, 0, 0, 0]
-           步骤2-当前 fit_contentxfit_content corner=None padding=None
-             圆点 24x24 corner=12 padding=None bg="rgba(24,144,255,1)"
-             container fit_contentxfit_content corner=None padding=[4, 0, 0, 0]
-           步骤3-待进行 fit_contentxfit_content corner=None padding=None
-             圆点 24x24 corner=12 padding=None bg="rgba(240,240,240,1)"
-             container fit_contentxfit_content corner=None padding=[4, 0, 0, 0]
-           步骤4-待进行 fit_contentxfit_content corner=None padding=None
-             圆点 24x24 corner=12 padding=None bg="rgba(240,240,240,1)"
-             container fit_contentxfit_content corner=None padding=[4, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-       container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-         套餐卡片1 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-           卡片头部 fill_containerxfit_content corner=None padding=None
-             状态标签 fit_contentx22 corner=11 padding=[0, 8, 0, 8] bg="rgba(246,255,237,1)"
-           container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-             处理方式选择器 fill_containerx48 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 168.91667556762695]
-       container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-         套餐卡片2 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-           卡片头部 fill_containerxfit_content corner=None padding=None
-             状态标签 fit_contentx22 corner=11 padding=[0, 8, 0, 8] bg="rgba(246,255,237,1)"
-           container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-             处理方式选择器-已选 fill_containerx48 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 210.91667556762695]
-       container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-         套餐卡片3 fill_containerxfit_content corner=12 padding=16 bg="rgba(255,255,255,1)"
-           卡片头部 fill_containerxfit_content corner=None padding=None
-             状态标签 fit_contentx22 corner=11 padding=[0, 8, 0, 8] bg="rgba(246,255,237,1)"
-           container fill_containerxfit_content corner=None padding=[4, 0, 0, 0]
-           container fill_containerxfit_content corner=None padding=[12, 0, 0, 0]
-             处理方式选择器-已选 fill_containerx48 corner=8 padding=[0, 12, 0, 12] bg="rgba(255,255,255,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 210.91667556762695]
-           container fill_containerxfit_content corner=None padding=[8, 0, 0, 0]
-             转新教练搜索区 fill_containerx56 corner=8 padding=[0, 12, 0, 12] bg="rgba(245,247,250,1)"
-               container fit_contentxfit_content corner=None padding=[0, 0, 0, 8]
-     底部提交栏 fill_containerx64 corner=None padding=[0, 16, 0, 16] bg="rgba(255,255,255,1)"
-       统计 fill_containerxfit_content corner=None padding=None
-       提交审批按钮 120x44 corner=8 padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0, "y1": 1.1102230246251565e-16, "x2": 1, "y2

### SVG/图标


---

## C-wechat-auth-page

- 尺寸: 375 x fit_content
- 布局: vertical, align: start, justify: start
- 子节点数: 1

### 文本内容

-           [图层]: leyoSwimming  (fontSize=24px, fill=#FFFFFF, w=fill_container)
-           [图层]: 专业游泳约课平台  (fontSize=14px, fill=#FFFFFFD8, w=fill_container)
-           [图层]: 登录后即可预约课程、购买套餐  (fontSize=14px, fill=#FFFFFFCC, w=fill_container)
-             [图层]: 已阅读并同意  (fontSize=12px, fill=#8C8C8C, w=73)
-             [图层]: 《隐私协议》  (fontSize=12px, fill=#1890FF, w=73)
-             [图层]: 《用户须知》  (fontSize=12px, fill=#1890FF, w=73)
-           [图层]:   (fontSize=22px, w=24)
-             [图层]: 微信一键登录  (fontSize=16px, fill=#FFFFFF, w=97)
-           [图层]: 使用手机号登录  (fontSize=14px, fill=#1890FF, w=fill_container)

### 主要 Frame 容器

- C-微信授权页 375xfit_content corner=None padding=None
-   页面容器 375x812 corner=None padding=None bg={"type": "linearGradient", "gradientUnits": "percentage", "coords": {"x1": 0.49999999999999994, "y1": 0, "x2": 0.5, "y2"
-     品牌区 fill_containerxfill_container corner=None padding=None
-       品牌Logo 96x96 corner=48 padding=None bg="rgba(255,255,255,0.15)"
-         Logo图标 fit_contentxfit_content corner=None padding=None
-       container fit_contentxfit_content corner=None padding=[24, 0, 0, 0]
-         品牌名称 177xfit_content corner=None padding=None
-       container fit_contentxfit_content corner=None padding=[8, 0, 0, 0]
-         品牌Slogan 112xfit_content corner=None padding=None
-       container fit_contentxfit_content corner=None padding=[48, 0, 0, 0]
-         登录引导文案 196xfit_content corner=None padding=None
-     底部登录区 fill_containerx180 corner=[16, 16, 0, 0] padding=[24, 16, 0, 16] bg="rgba(255,255,255,1)"
-       隐私协议勾选区 fill_containerx40 corner=None padding=None
-         container fit_contentxfit_content corner=None padding=[0, 0, 0, 8]
-           协议文案 fit_contentxfit_content corner=None padding=None
-       container fill_containerxfit_content corner=None padding=[16, 0, 0, 0]
-         微信一键登录按钮 fill_containerx48 corner=8 padding=None bg="rgba(7,193,96,1)"
-           container fit_contentxfit_content corner=None padding=[0, 0, 0, 8.000003814697266]
-       container fit_contentxfit_content corner=None padding=[16, 0, 0, 0]
-         手机号登录入口 98xfit_content corner=None padding=None

### SVG/图标

-           矢量 64x64 fills=None stroke={'align': 'center', 'thickness': 1.2, 'lineCap': 'butt', 'lineJoin': 'miter', 'fills': 'rgba(255,255,255,0.3)'}
-           矢量 28x37 fills=None stroke={'align': 'center', 'thickness': 3, 'lineCap': 'round', 'lineJoin': 'miter', 'fills': '#FFFFFF'}
-           矢量 9x9 fills=#FF6B35 stroke=None

---
