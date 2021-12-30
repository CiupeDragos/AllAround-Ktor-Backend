package com.example.requests

import com.example.data.models.BaseModel
import com.example.util.Constants.TYPE_JOIN_GROUP_REQUEST

data class JoinGroupRequest(
    val username: String,
    val groupId: String
): BaseModel(TYPE_JOIN_GROUP_REQUEST)
