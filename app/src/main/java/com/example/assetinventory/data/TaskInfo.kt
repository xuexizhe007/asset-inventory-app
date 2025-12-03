package com.example.assetinventory.data

/**
 * 任务信息
 *
 * @property id 任务ID
 * @property name 任务名称
 * @property assetCount 资产数量
 * @property createdAt 创建时间（导入时间，毫秒时间戳）
 */
data class TaskInfo(
    val id: Long,
    val name: String,
    val assetCount: Int,
    val createdAt: Long
)
