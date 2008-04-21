create table tpcc..new_order (
    no_o_id		integer,
    no_d_id		integer,
    no_w_id		integer,
    primary key (no_w_id, no_d_id, no_o_id)
)
