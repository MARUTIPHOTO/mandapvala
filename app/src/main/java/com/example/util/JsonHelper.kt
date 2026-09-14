package com.example.util

import com.example.data.model.BillItem
import org.json.JSONArray
import org.json.JSONObject

object JsonHelper {

    fun itemsToJson(items: List<BillItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("itemId", item.itemId)
            obj.put("title", item.title)
            obj.put("category", item.category)
            obj.put("imageUrl", item.imageUrl)
            obj.put("quantity", item.quantity)
            obj.put("price", item.price)
            obj.put("lineTotal", item.lineTotal)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToItems(json: String?): List<BillItem> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<BillItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BillItem(
                        itemId = obj.optLong("itemId", 0),
                        title = obj.optString("title", "Item"),
                        category = obj.optString("category", "Other"),
                        imageUrl = obj.optString("imageUrl", ""),
                        quantity = obj.optInt("quantity", 1),
                        price = obj.optDouble("price", 0.0),
                        lineTotal = obj.optDouble("lineTotal", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
