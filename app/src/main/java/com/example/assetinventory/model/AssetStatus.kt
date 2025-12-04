package com.example.assetinventory.model

enum class AssetStatus(val displayName: String) {
    UNCHECKED("未盘点"),
    MATCHED("相符"),
    MISMATCH("不相符"),
    LABEL_REPRINT("补打标签");

    companion object {
        fun fromDisplayName(name: String?): AssetStatus? =
            values().firstOrNull { it.displayName == name }
    }
}
