package com.medassist.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicineView(
    val id: String,
    val name: String,
    val saltComposition: String? = null,
    val manufacturer: String? = null,
    val dosageForm: String? = null,
    val strength: String? = null,
    val packSize: String? = null,
    val schedule: DrugSchedule = DrugSchedule.OTC,
    val gstRate: Money? = null,
    val mrp: Money? = null,
    val therapeuticClass: String? = null,
    val description: String? = null,
    val prescriptionRequired: Boolean = false,
)

@Serializable
data class ProductView(
    val id: String,
    val medicineId: String? = null,
    val name: String,
    val sku: String? = null,
    val barcode: String? = null,
    val saltComposition: String? = null,
    val manufacturer: String? = null,
    val packSize: String? = null,
    val schedule: DrugSchedule = DrugSchedule.OTC,
    val prescriptionRequired: Boolean = false,
    val gstRate: Money,
    val mrp: Money,
    val sellingPrice: Money,
    val reorderLevel: Int = 0,
    val rackLocation: String? = null,
    val active: Boolean = true,
)

@Serializable
data class ImportRequest(val medicineIds: List<String>, val reorderLevel: Int? = null)

@Serializable
data class ImportResult(val imported: Int, val skippedAlreadyInCatalog: Int, val products: List<ProductView> = emptyList())
