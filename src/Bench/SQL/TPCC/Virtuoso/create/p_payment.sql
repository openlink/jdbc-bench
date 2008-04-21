create procedure payment (
    in _w_id integer,
    in _c_w_id integer,
    in h_amount float,
    in _d_id integer,
    in _c_d_id integer,
    in _c_id integer,
    in _c_last varchar)
{
  declare n, _w_ytd, _d_ytd, _c_cnt_payment integer; 
  declare
    _c_data, _c_first, _c_middle, 
    _c_street_1,  _c_street_2, _c_city, _c_state, _c_zip,
    _c_phone, _c_credit, _c_credit_lim,
    _c_discount, _c_balance, _c_since, _c_data_1, _c_data_2,
    _d_street_1, _d_street_2, _d_city, _d_state, _d_zip, _d_name,
    _w_street_1, _w_street_2, _w_city, _w_state, _w_zip, _w_name,
    screen_data varchar;

  if (_c_id = 0)
    {
      declare namecnt integer; 
      whenever not found goto no_customer; 

      select count(C_ID) into namecnt 
        from tpcc..customer
	where c_last = _c_last
	  and c_d_id = _d_id
	  and c_w_id = _w_id; 

      declare c_byname cursor for 
	select c_id
	  from tpcc..customer
	  where c_w_id = _c_w_id
	    and c_d_id = _c_d_id
	    and c_last = _c_last
	  order by c_w_id, c_d_id, c_last, c_first; 
    
      open c_byname (exclusive); 

      n := 0; 
      while (n <= namecnt / 2)
        {
	  fetch c_byname   into _c_id;
	  n := n + 1; 
	}

      close c_byname; 
    }

  declare c_cr cursor for
    select
      c_first, c_middle, c_last,
      c_street_1, c_street_2, c_city, c_state, c_zip, 
      c_phone, c_credit, c_credit_lim,
      c_discount, c_balance, c_since, c_data_1, c_data_2, c_cnt_payment
    from tpcc..customer
    where c_w_id = _c_w_id
      and c_d_id = _c_d_id
      and c_id = _c_id;

  open c_cr (exclusive);

  fetch c_cr into 
    _c_first, _c_middle, _c_last,
    _c_street_1, _c_street_2, _c_city, _c_state, _c_zip,
    _c_phone, _c_credit, _c_credit_lim,
    _c_discount, _c_balance, _c_since, _c_data_1, _c_data_2, _c_cnt_payment;
  
  _c_balance := _c_balance + h_amount; 

  if (_c_credit = 'BC')
    {
      update tpcc..customer set
        c_balance = _c_balance,  
	c_data_1 = bc_c_data (
	  sprintf ('%5d%5d%5d%5d%5d%9f', _c_id, _c_d_id, _c_w_id, _d_id,
	    _w_id, h_amount), _c_data_1),
	c_cnt_payment = _c_cnt_payment + 1
	where current of c_cr;

      screen_data := subseq (_c_data_1, 1, 200);
    }
  else
    {
      update tpcc..customer set
        c_balance = _c_balance,
	c_cnt_payment = _c_cnt_payment + 1
	where current of c_cr;

      screen_data := ' ';
    }

  declare d_cur cursor for
    select d_street_1, d_street_2, d_city, d_state, d_zip, d_name, d_ytd
    from tpcc..district
    where d_w_id = _w_id
      and d_id = _d_id; 

  open d_cur (exclusive); 

  fetch d_cur into _d_street_1, _d_street_2, _d_city, _d_state, _d_zip,
      _d_name, _d_ytd; 

  update tpcc..district set
    d_ytd = _d_ytd + h_amount
    where current of d_cur; 

  close d_cur; 

  declare w_cur cursor for 
    select  w_street_1, w_street_2, w_city, w_state, w_zip, w_name, w_ytd
    from tpcc..warehouse
    where w_id = _w_id; 

  open w_cur (exclusive); 

  fetch	 w_cur into _w_street_1, _w_street_2, _w_city, _w_state, _w_zip,
      _w_name, _w_ytd; 

  update tpcc..warehouse set w_ytd = _w_ytd + h_amount; 

  declare h_data varchar; 
  h_data := _w_name; 

  insert into tpcc..history (
      h_c_d_id, h_c_w_id, h_c_id, h_d_id, h_w_id, h_date, h_amount, h_data) 
    values (_c_d_id, _c_w_id, _c_id, _d_id, _w_id, now (), h_amount, h_data); 
  
  result ( _c_id,
           _c_last,
           now (),
           _w_street_1,
           _w_street_2,
           _w_city,
           _w_state,
           _w_zip,
           _d_street_1,
           _d_street_2,
           _d_city,
           _d_state,
           _d_zip,
           _c_first,
           _c_middle,
           _c_street_1,
           _c_street_2,
           _c_city,
           _c_state,
           _c_zip,
           _c_phone,
           _c_since,
           _c_credit,
           _c_credit_lim,
           _c_discount,
           _c_balance,
           screen_data);
  return;

no_customer:
  dbg_printf ('No customer %s %d.\n', _c_last, _c_id); 
  signal ('NOCUS', 'No customer in payment.');
}
