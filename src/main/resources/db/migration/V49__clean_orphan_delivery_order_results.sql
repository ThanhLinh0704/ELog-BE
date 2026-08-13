-- V49: Clean orphan rows from delivery_order_results left behind by seed refreshes
DELETE dor FROM delivery_order_results dor
LEFT JOIN trip_draft_stops tds ON dor.stop_id = tds.id
LEFT JOIN orders o ON dor.order_id = o.id
LEFT JOIN trip_executions te ON dor.trip_execution_id = te.id
WHERE tds.id IS NULL OR o.id IS NULL OR te.id IS NULL;
