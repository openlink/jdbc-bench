create table tpcc..orders (
    o_id		integer,
    o_d_id		integer,
    o_w_id		integer,
    o_c_id		integer,
    o_entry_d		date,
    o_carrier_id	integer,
    o_ol_cnt		integer,
    o_all_local		integer,
    primary key (o_w_id, o_d_id, o_id)
)
