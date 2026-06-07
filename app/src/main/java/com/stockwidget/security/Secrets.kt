package com.stockwidget.security

object Secrets {
    init {
        System.loadLibrary("stockwidget")
    }

    external fun getFinnhubApiKey(): String
}