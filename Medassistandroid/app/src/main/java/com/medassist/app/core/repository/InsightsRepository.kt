package com.medassist.app.core.repository

import com.medassist.app.core.model.Dashboard
import com.medassist.app.core.model.GstSummary
import com.medassist.app.core.model.InboxItem
import com.medassist.app.core.model.PageResponse
import com.medassist.app.core.model.SalesSummary
import com.medassist.app.core.model.UnreadCount
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

/** Reports for staff and the notification inbox for everyone. */
class InsightsRepository(private val client: HttpClient) {

    suspend fun dashboard(branchId: String?): Result<Dashboard> = apiCall {
        client.get("reports/dashboard") { parameter("branchId", branchId) }.body()
    }

    /** Dates are ISO yyyy-MM-dd Indian business dates. */
    suspend fun sales(from: String, to: String): Result<SalesSummary> = apiCall {
        client.get("reports/sales") {
            parameter("from", from)
            parameter("to", to)
        }.body()
    }

    suspend fun gst(from: String, to: String): Result<GstSummary> = apiCall {
        client.get("reports/gst") {
            parameter("from", from)
            parameter("to", to)
        }.body()
    }

    suspend fun inbox(page: Int = 0): Result<PageResponse<InboxItem>> = apiCall {
        client.get("notifications") { parameter("page", page) }.body()
    }

    suspend fun unreadCount(): Result<Long> = apiCall { client.get("notifications/unread-count").body<UnreadCount>().unread }

    suspend fun markAllRead(): Result<Unit> = apiCall {
        client.post("notifications/read-all")
        Unit
    }
}
