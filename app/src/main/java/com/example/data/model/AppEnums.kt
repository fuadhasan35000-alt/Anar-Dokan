package com.example.data.model

enum class UserRole(val banglaTitle: String) {
    SUPER_ADMIN("সুপার অ্যাডমিন"),
    ADMIN("অ্যাডমিন"),
    STAFF("স্টাফ")
}

enum class UserStatus(val banglaTitle: String) {
    PENDING("অপেক্ষমান"),
    APPROVED("অনুমোদিত"),
    REJECTED("বাতিলকৃত"),
    DISABLED("নিষ্ক্রিয়")
}

enum class StockChangeType(val banglaTitle: String) {
    STOCK_IN("স্টক ইন"),
    STOCK_OUT("স্টক আউট"),
    ADJUSTMENT("সমন্বয়"),
    SALE("বিক্রি"),
    RETURN("ফেরত")
}

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

enum class PaymentMethod(val banglaTitle: String) {
    CASH("নগদ (Cash)"),
    BKASH("বিকাশ (bKash)"),
    NAGAD("নগদ (Nagad)"),
    ROCKET("রকেট (Rocket)"),
    BANK("ব্যাংক ট্রান্সফার"),
    CARD("কার্ড")
}

enum class ExpenseCategory(val banglaTitle: String) {
    SHOP_RENT("দোকান ভাড়া"),
    ELECTRICITY("বিদ্যুৎ বিল"),
    TRANSPORT("পরিবহন"),
    SALARY("বেতন"),
    PURCHASE("পণ্য ক্রয়"),
    TEA_SNACKS("আপ্যায়ন"),
    MAINTENANCE("মেরামত"),
    OTHER("অন্যান্য")
}

enum class AuditAction(val banglaTitle: String) {
    LOGIN("লগইন"),
    LOGOUT("লগআউট"),
    PRODUCT_CREATE("পণ্য তৈরি"),
    PRODUCT_UPDATE("পণ্য আপডেট"),
    PRODUCT_DELETE("পণ্য মুছে ফেলা"),
    STOCK_IN("স্টক ইন"),
    STOCK_OUT("স্টক আউট"),
    STOCK_ADJUSTMENT("স্টক সমন্বয়"),
    SALE_CREATE("বিক্রি তৈরি"),
    PAYMENT_RECORD("পেমেন্ট গ্রহণ"),
    EXPENSE_ADD("খরচ যোগ"),
    USER_CREATE("ব্যবহারকারী তৈরি"),
    USER_APPROVE("স্টাফ অনুমোদন"),
    USER_REJECT("স্টাফ বাতিল"),
    USER_DISABLE("ব্যবহারকারী নিষ্ক্রিয়"),
    PERMISSION_CHANGE("অনুমতি পরিবর্তন"),
    BUSINESS_CREATE("ব্যবসা তৈরি"),
    BRANCH_CREATE("শাখা তৈরি"),
    BACKUP("ব্যাকআপ গ্রহণ"),
    RESTORE("রিস্টোর সম্পন্ন")
}

enum class ShopPermission(val banglaTitle: String, val description: String) {
    VIEW_STOCK("স্টক দেখা", "পণ্যের তালিকা ও স্টক দেখার অনুমতি"),
    EDIT_STOCK("স্টক পরিবর্তন", "স্টক ইন, স্টক আউট ও সমন্বয়ের অনুমতি"),
    ADD_PRODUCT("নতুন পণ্য যোগ", "নতুন পণ্য ক্যাটালগে যুক্ত করার অনুমতি"),
    EDIT_PRODUCT("পণ্য এডিট", "পণ্যের দাম ও বিবরণ পরিবর্তনের অনুমতি"),
    DELETE_PRODUCT("পণ্য মুছে ফেলা", "পণ্য ডিলিট করার অনুমতি"),
    CREATE_SALE("বিক্রি করা", "POS বিক্রি সম্পন্ন করার অনুমতি"),
    VIEW_SALES("বিক্রি ইতিহাস দেখা", "পূর্বের বিক্রয় রশিদ দেখার অনুমতি"),
    VIEW_CUSTOMERS("কাস্টমার দেখা", "কাস্টমার তালিকা দেখার অনুমতি"),
    EDIT_CUSTOMERS("কাস্টমার এডিট", "কাস্টমারের তথ্য যোগ ও এডিট করার অনুমতি"),
    DELETE_CUSTOMERS("কাস্টমার ডিলিট", "কাস্টমার প্রোফাইল ডিলিট করার অনুমতি"),
    RECORD_PAYMENT("পেমেন্ট গ্রহণ", "বকেয়া আদায় ও পেমেন্ট রেকর্ড করার অনুমতি"),
    VIEW_DUE("দেনা-পাওনা দেখা", "কাস্টমারের বকেয়া তালিকা দেখার অনুমতি"),
    VIEW_REPORTS("রিপোর্ট দেখা", "দৈনিক ও মাসিক লাভ-ক্ষতির রিপোর্ট দেখার অনুমতি"),
    MANAGE_EXPENSES("খরচ ব্যবস্থাপনা", "দোকানের যাবতীয় খরচ যোগ ও এডিটের অনুমতি"),
    SEND_SMS("SMS পাঠান", "কাস্টমারকে বকেয়া রিমাইন্ডার SMS পাঠানোর অনুমতি"),
    SEND_WHATSAPP("WhatsApp মেসেজ", "WhatsApp এ ইনভয়েস ও রিমাইন্ডার পাঠানোর অনুমতি"),
    VIEW_STAFF("স্টাফ দেখা", "শাখার স্টাফদের তালিকা দেখার অনুমতি"),
    MANAGE_STAFF("স্টাফ নিয়ন্ত্রণ", "স্টাফ অনুমোদন ও ভূমিকা পরিবর্তনের অনুমতি"),
    MANAGE_BRANCH("শাখা নিয়ন্ত্রণ", "নতুন শাখা তৈরি ও পরিবর্তনের অনুমতি"),
    BACKUP_DATA("ডাটা ব্যাকআপ", "দোকানের ডাটা এক্সপোর্ট করার অনুমতি"),
    RESTORE_DATA("ডাটা রিস্টোর", "ব্যাকআপ ফাইল থেকে ডাটা রিস্টোর করার অনুমতি")
}
