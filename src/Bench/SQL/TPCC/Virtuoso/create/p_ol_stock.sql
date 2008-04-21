create procedure ol_stock (
    in _w_id integer,
    in d_id integer,
    inout _ol_i_id integer,
    in _ol_supply_w_id integer,
    in qty integer,
    out amount float,
    inout s_dist_01 varchar, 
    inout s_dist_02 varchar, 
    inout s_dist_03 varchar, 
    inout s_dist_04 varchar, 
    inout s_dist_05 varchar, 
    inout s_dist_06 varchar, 
    inout s_dist_07 varchar, 
    inout s_dist_08 varchar, 
    inout s_dist_09 varchar, 
    inout s_dist_10 varchar,
    inout dist_info varchar)
{
  declare _s_data varchar;
  declare _s_quantity, _s_cnt_order, _s_cnt_remote integer; 
  declare _i_name varchar;

  if (_ol_i_id = 0) return; 

  whenever not found goto no_item; 
  select i_price, i_name into amount, _i_name
    from tpcc..item
    where i_id = _ol_i_id; 

  declare s_cur cursor for
    select s_quantity, s_data, s_cnt_order, s_cnt_remote,
        s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
	s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10
    from tpcc..stock
    where s_i_id = _ol_i_id
      and s_w_id = _ol_supply_w_id; 

  whenever not found goto no_stock; 

  open s_cur (exclusive); 
  fetch s_cur into
      _s_quantity, _s_data, _s_cnt_order, _s_cnt_remote,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10; 

  if (_s_quantity < qty)
    _s_quantity := _s_quantity - qty + 91; 
  else
    _s_quantity := _s_quantity - qty; 

  if (_w_id <> _ol_supply_w_id)
    _s_cnt_remote := _s_cnt_remote + 1;

  update tpcc..stock set
    s_quantity = _s_quantity,
    s_cnt_order = _s_cnt_order + 1,
    s_cnt_remote = _s_cnt_remote
    where current of s_cur; 
  
       if (d_id = 1) dist_info := s_dist_01;
  else if (d_id = 2) dist_info := s_dist_02;
  else if (d_id = 3) dist_info := s_dist_03;
  else if (d_id = 4) dist_info := s_dist_04;
  else if (d_id = 5) dist_info := s_dist_05;
  else if (d_id = 6) dist_info := s_dist_06;
  else if (d_id = 7) dist_info := s_dist_07;
  else if (d_id = 8) dist_info := s_dist_08;
  else if (d_id = 9) dist_info := s_dist_09;
  else if (d_id = 10) dist_info := s_dist_10;

  result (_i_name, _s_quantity, 'G', amount, amount * qty);

  amount := qty * amount; 
  
  return; 
no_stock:
  signal ('NOSTK', 'No stock row found.'); 

no_item:
  signal ('NOITM', 'No item row found.'); 
}
