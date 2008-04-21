create table tpcc..district (
    d_id		integer,
    d_w_id		integer,
    d_name		character (10),
    d_street_1		character (20),
    d_street_2		character (20),
    d_city		character (20),
    d_state		character (2),
    d_zip		character (9),
    d_tax		numeric (4,2),
    d_ytd		numeric,
    d_next_o_id		integer,
    primary key (d_w_id, d_id)
)
