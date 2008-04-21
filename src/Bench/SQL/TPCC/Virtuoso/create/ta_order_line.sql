create table tpcc..order_line (
    ol_o_id		integer,
    ol_d_id		integer,
    ol_w_id		integer,
    ol_number		integer,
    ol_i_id		integer,
    ol_supply_w_id	integer,
    ol_delivery_d	date,
    ol_quantity		integer,
    ol_amount		numeric,
    ol_dist_info	character (24),
    primary key (ol_w_id, ol_d_id, ol_o_id, ol_number)
)
