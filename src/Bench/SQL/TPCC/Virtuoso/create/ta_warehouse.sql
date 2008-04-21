create table tpcc..warehouse (
    w_id		integer, 
    w_name		character (10),
    w_street_1		character (20),
    w_street_2		character (20),
    w_city		character (20),
    w_state		character (2),
    w_zip		character (9),
    w_tax		numeric (4,2),
    w_ytd		numeric,
    primary key (w_id)
)
