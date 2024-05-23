package com.xjl.emp3recorder.logger

interface LoggerInterface {
    fun d(tag:String,content: String)
    fun i(tag:String,content: String)
    fun w(tag:String,content: String)
    fun e(tag:String,content: String)
}