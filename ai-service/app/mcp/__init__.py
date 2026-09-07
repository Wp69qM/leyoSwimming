"""MCP（Model Context Protocol）能力包。

将知识库检索与业务推荐能力通过 MCP Streamable HTTP 协议对外暴露。
独立于 INTERNAL_API_TOKEN 的 Bearer 鉴权（auth.py），
工具注册收敛在 server.py（逐个显式注册，无批量导入路径）。
"""
