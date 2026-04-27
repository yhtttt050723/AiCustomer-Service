package com.ragask.ticketing.service;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class TicketCategoryService {

    public static final List<String> CATEGORIES = List.of(
            "人事/假期",
            "采购/订单",
            "库存/同步",
            "发版/回滚",
            "账号/权限",
            "其他"
    );

    /**
     * Lightweight, deterministic classifier for bootstrap.
     * - Prefer precision on obvious keywords.
     * - Return null when uncertain so L1 can classify.
     */
    public String classify(String question, List<String> citations, String answer) {
        String q = normalize(question);
        String c = normalize(String.join(" ", citations == null ? List.of() : citations));
        String a = normalize(answer);
        String all = q + " " + c + " " + a;

        if (containsAny(all, "请假", "假期", "病假", "年假", "调休", "oa")) {
            return "人事/假期";
        }
        if (containsAny(all, "po号", "采购", "订单", "采购申请", "po-")) {
            return "采购/订单";
        }
        if (containsAny(all, "sku", "同步", "库存", "消息队列", "mq")) {
            return "库存/同步";
        }
        if (containsAny(all, "发版", "发布", "回滚", "灰度", "版本")) {
            return "发版/回滚";
        }
        if (containsAny(all, "账号", "登录", "锁定", "密码", "权限", "rbac", "解锁")) {
            return "账号/权限";
        }

        // Unknown: require manual category only when handing off.
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(normalize(k))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}

