package com.booking.platform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 國際化服務
 *
 * <p>提供多語系訊息取得功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    /**
     * 取得訊息（使用當前語系）
     *
     * @param code 訊息代碼
     * @return 訊息內容
     */
    public String getMessage(String code) {
        return getMessage(code, (Object[]) null);
    }

    /**
     * 取得訊息（使用當前語系，帶參數）
     *
     * @param code 訊息代碼
     * @param args 參數
     * @return 訊息內容
     */
    public String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale());
    }

    /**
     * 取得訊息（指定語系）
     *
     * @param code   訊息代碼
     * @param locale 語系
     * @return 訊息內容
     */
    public String getMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }

    /**
     * 取得訊息（指定語系，帶參數）
     *
     * @param code   訊息代碼
     * @param args   參數
     * @param locale 語系
     * @return 訊息內容
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * 取得當前語系
     *
     * @return 當前語系
     */
    public Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    /**
     * 取得語系名稱
     *
     * @return 語系名稱（如：zh_TW, zh_CN, en）
     */
    public String getCurrentLanguage() {
        Locale locale = getCurrentLocale();
        if (locale.equals(Locale.TAIWAN) || locale.equals(Locale.TRADITIONAL_CHINESE)) {
            return "zh_TW";
        } else if (locale.equals(Locale.CHINA) || locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            return "zh_CN";
        } else if (locale.equals(Locale.ENGLISH) || locale.getLanguage().equals("en")) {
            return "en";
        }
        return locale.toString();
    }

    /**
     * 取得支援的語系列表
     *
     * @return 語系代碼陣列
     */
    public String[] getSupportedLanguages() {
        return new String[]{"zh_TW", "zh_CN", "en"};
    }

    /**
     * 取得語系顯示名稱
     *
     * @param languageCode 語系代碼
     * @return 顯示名稱
     */
    public String getLanguageDisplayName(String languageCode) {
        return switch (languageCode) {
            case "zh_TW" -> "繁體中文";
            case "zh_CN" -> "简体中文";
            case "en" -> "English";
            default -> languageCode;
        };
    }
}
