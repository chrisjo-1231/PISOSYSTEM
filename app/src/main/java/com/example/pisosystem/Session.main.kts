#!/usr/bin/env kotlin

package com.example.pisosystem.model

data class Session(
    val mac_address: String,
    val ip_address: String,
    val remaining_minutes: Int
)