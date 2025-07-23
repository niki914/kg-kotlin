package utils

class ConfigNotSetException(unsetConfig: String) : Exception("未正确配置 $unsetConfig, 程序将终止")