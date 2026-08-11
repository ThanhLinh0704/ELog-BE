-- Temporarily shift sequence_order to avoid unique constraint (uq_route_stops_seq) violation during updates
UPDATE route_stops SET sequence_order = sequence_order + 100 WHERE route_id = 1;

-- Optimize sequence_order for route RT-T01 (route_id = 1) to minimize return distance to warehouse
UPDATE route_stops SET sequence_order = 1 WHERE route_id = 1 AND store_id = 175; -- KH0199
UPDATE route_stops SET sequence_order = 2 WHERE route_id = 1 AND store_id = 136; -- KH0138
UPDATE route_stops SET sequence_order = 3 WHERE route_id = 1 AND store_id = 145; -- KH0147
UPDATE route_stops SET sequence_order = 4 WHERE route_id = 1 AND store_id = 142; -- KH0144
UPDATE route_stops SET sequence_order = 5 WHERE route_id = 1 AND store_id = 138; -- KH0140
UPDATE route_stops SET sequence_order = 6 WHERE route_id = 1 AND store_id = 147; -- KH0149
UPDATE route_stops SET sequence_order = 7 WHERE route_id = 1 AND store_id = 150; -- KH0152
UPDATE route_stops SET sequence_order = 8 WHERE route_id = 1 AND store_id = 137; -- KH0139
UPDATE route_stops SET sequence_order = 9 WHERE route_id = 1 AND store_id = 139; -- KH0141
UPDATE route_stops SET sequence_order = 10 WHERE route_id = 1 AND store_id = 176; -- KH0200
UPDATE route_stops SET sequence_order = 11 WHERE route_id = 1 AND store_id = 174; -- KH0198

-- Invalidate cached polyline and distance on all routes so they recalculate with full multi-stop polylines on next view
UPDATE routes SET route_polyline = NULL, total_distance_km = NULL, total_duration_min = NULL;
