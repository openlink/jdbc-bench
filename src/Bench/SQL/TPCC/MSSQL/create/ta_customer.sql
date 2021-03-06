create table tpcc..customer (
		       c_id           int,
		       c_d_id         tinyint,
		       c_w_id         smallint,
		       c_first        char(16),
		       c_middle       char(2),
		       c_last         char(16),
		       c_street_1     char(20),
		       c_street_2     char(20),
		       c_city         char(20),
		       c_state        char(2),
		       c_zip          char(9),
		       c_phone        char(16),
		       c_since        datetime,
		       c_credit       char(2),
		       c_credit_lim   float,
		       c_discount     float,
		       c_balance      float,
		       c_ytd_payment  float,
		       c_cnt_payment  smallint,
		       c_cnt_delivery smallint,
		       c_data_1       char(250),
		       c_data_2       char(250))
