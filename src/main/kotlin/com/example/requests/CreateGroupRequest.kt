package com.example.requests

data class CreateGroupRequest(
    val name: String,
    val members: List<String>
)
