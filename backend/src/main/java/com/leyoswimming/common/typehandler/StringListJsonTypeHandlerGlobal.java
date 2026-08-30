package com.leyoswimming.common.typehandler;

import com.leyoswimming.common.StringListJsonTypeHandler;

/**
 * 全局注册的 {@link StringListJsonTypeHandler} 子类，用于处理未显式标注 typeHandler 的
 * {@code List<String>} 字段（如 package_template.tags/images）。
 *
 * <p>通过仅扫描 {@code com.leyoswimming.common.typehandler} 子包，避免把
 * {@code CoachCertificateListJsonTypeHandler} 也注册为全局 List 处理器而产生冲突。
 */
public class StringListJsonTypeHandlerGlobal extends StringListJsonTypeHandler {}
