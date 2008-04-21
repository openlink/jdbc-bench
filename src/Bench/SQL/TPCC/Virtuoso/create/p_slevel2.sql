create procedure slevel2 (
    in w_id integer,
    in _d_id integer,
    in threshold integer)
{
  declare last_o, n_items integer; 

  select d_next_o_id into last_o
    from tpcc..district
    where d_w_id = w_id and d_id = _d_id; 

  select count (*) into n_items
    from (select distinct ol_i_id from tpcc..order_line
	  where ol_w_id = w_id
	  and ol_d_id = _d_id
	  and ol_o_id < last_o
	  and ol_o_id >= last_o - 20) O,
        tpcc..stock
      where
	s_w_id = w_id
	  and s_i_id = ol_i_id
	    and s_quantity < threshold; 
  
  result_names (n_items);
  result (n_items);
}
