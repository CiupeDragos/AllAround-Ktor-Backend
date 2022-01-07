package com.example.responses

data class FriendRequestsResponse(
    val sentRequests: List<String>,
    val receivedRequests: List<String>
)
