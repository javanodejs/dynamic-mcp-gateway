INSERT INTO mcp_service (id, service_code, service_name, service_desc, protocol, enabled, request_headers, created_at, updated_at) VALUES
(1, 'shop', 'E-commerce MCP Service', 'E-commerce related tools service', 'STATELESS', 1, '{"x-tenant-id":"shop-tenant","x-source":"mcp-gateway"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'logistics', 'Logistics MCP Service', 'Logistics related tools service', 'STATELESS', 1, '{"x-tenant-id":"logistics-tenant","x-source":"mcp-gateway"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO mcp_tool (id, tool_code, tool_name, tool_desc, http_method, api_url, parameters_json, request_headers, enabled, created_at, updated_at) VALUES
(1, 'goods.list', 'Query Product List', 'Query e-commerce product list', 'GET', 'https://httpbin.org/get',
 '[{"name":"page","required":false,"type":"integer","description":"Page number","defaultValue":1},{"name":"pageSize","required":false,"type":"integer","description":"Page size","defaultValue":20}]',
 '{"x-tool":"goods-list"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'logistics.track', 'Query Logistics Tracking', 'Query logistics tracking by order number', 'GET', 'https://httpbin.org/get',
 '[{"name":"orderNo","required":true,"type":"string","description":"Order number","defaultValue":null}]',
 '{"x-tool":"logistics-track"}', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO mcp_service_tool_binding (id, service_id, tool_id, created_at, updated_at) VALUES
(1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);