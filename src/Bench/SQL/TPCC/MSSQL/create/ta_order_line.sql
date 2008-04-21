create table tpcc..order_line (
			 ol_o_id         int,
			 ol_d_id         tinyint,
			 ol_w_id         smallint,
			 ol_number       tinyint,
			 ol_i_id         int,
			 ol_supply_w_id  smallint,
			 ol_delivery_d   datetime,
			 ol_quantity     smallint,
			 ol_amount       float,
			 ol_dist_info    char(24))
