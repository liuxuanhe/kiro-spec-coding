package com.parking.common;

import java.util.regex.Pattern;

/**
 * 车牌格式验证工具类
 * 支持中国标准车牌格式验证，包括普通车牌（7位）和新能源车牌（8位）
 * Validates: Requirements 3.2
 */
public final class CarPlateValidator {

    /**
     * 省份简称集合
     */
    private static final String PROVINCES = "京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁";

    /**
     * 中国车牌格式正则表达式
     * 规则：
     *   第1位：省份简称
     *   第2位：字母 A-Z
     *   普通车牌后5位：字母和数字混合（至少包含1个数字）
     *   新能源车牌后6位：字母和数字混合
     */
    private static final Pattern CAR_PLATE_PATTERN = Pattern.compile(
            "^[" + PROVINCES + "][A-Z]" +
            "(" +
                // 普通车牌：后5位字母数字混合，至少1个数字
                "(?=[A-Z0-9]{5}$)(?=.*[0-9])[A-Z0-9]{5}" +
                "|" +
                // 新能源车牌：后6位字母数字混合
                "[A-Z0-9]{6}" +
            ")$"
    );

    private CarPlateValidator() {
        // 工具类禁止实例化
    }

    /**
     * 验证车牌格式是否有效
     *
     * @param carNumber 待验证的车牌号
     * @return true-格式有效，false-格式无效
     */
    public static boolean isValid(String carNumber) {
        if (carNumber == null || carNumber.isEmpty()) {
            return false;
        }
        return CAR_PLATE_PATTERN.matcher(carNumber).matches();
    }

    /**
     * 验证车牌格式，如果无效则抛出 BusinessException
     *
     * @param carNumber 待验证的车牌号
     * @throws BusinessException 如果车牌格式无效，抛出 PARAM_ERROR
     */
    public static void validate(String carNumber) {
        if (!isValid(carNumber)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
    }
}
