package com.example.assetinventory.model

import com.example.assetinventory.R

enum class AssetStatus(val displayName: String, val colorResId: Int) {
    UNCHECKED("未盘点", R.color.status_unchecked),
    MATCHED("相符", R.color.status_matched),
    MISMATCH("不相符", R.color.status_mismatch),
    LABEL_REPRINT("补打标签", R.color.status_reprint);

    companion object {
        fun fromDisplayName(name: String?): AssetStatus? =
            values().firstOrNull { it.displayName == name }
    }
}
