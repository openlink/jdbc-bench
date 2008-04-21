create procedure new_order (
    in _w_id integer, in _d_id integer, in _c_id integer,
    in o_ol_cnt integer, in o_all_local integer,
    in i_id_1 integer, in s_w_id_1 integer, in qty_1 integer,
    in i_id_2 integer, in s_w_id_2 integer, in qty_2 integer,
    in i_id_3 integer, in s_w_id_3 integer, in qty_3 integer,
    in i_id_4 integer, in s_w_id_4 integer, in qty_4 integer,
    in i_id_5 integer, in s_w_id_5 integer, in qty_5 integer,
    in i_id_6 integer, in s_w_id_6 integer, in qty_6 integer,
    in i_id_7 integer, in s_w_id_7 integer, in qty_7 integer,
    in i_id_8 integer, in s_w_id_8 integer, in qty_8 integer,
    in i_id_9 integer, in s_w_id_9 integer, in qty_9 integer,
    in i_id_10 integer, in s_w_id_10 integer, in qty_10 integer
    )
{
  declare
    ol_a_1, ol_a_2, ol_a_3, ol_a_4, ol_a_5,
    ol_a_6, ol_a_7, ol_a_8, ol_a_9, ol_a_10 integer; 
  declare _c_discount, _d_tax, _w_tax, tax_and_discount float; 
  declare _datetime timestamp;
  declare _c_last, _c_credit varchar; 
  declare _o_id integer; 
  declare
    i_name, s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05, s_dist_06,
    s_dist_07, s_dist_08, s_dist_09, s_dist_10,
    disti_1, disti_2, disti_3, disti_4, disti_5, disti_6, disti_7, disti_8,
    disti_9, disti_10 varchar;

  _datetime := now ();

  result_names (i_name, qty_1, disti_1, ol_a_1, ol_a_2);

  ol_stock (
      _w_id, _d_id, i_id_1, s_w_id_1, qty_1, ol_a_1,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_1); 

  ol_stock (
      _w_id, _d_id, i_id_2, s_w_id_2, qty_2, ol_a_2,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_2); 

  ol_stock (
      _w_id, _d_id, i_id_3, s_w_id_3, qty_3, ol_a_3,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_3); 

  ol_stock (
      _w_id, _d_id, i_id_4, s_w_id_4, qty_4, ol_a_4,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_4); 

  ol_stock (
      _w_id, _d_id, i_id_5, s_w_id_5, qty_5, ol_a_5,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_5); 

  ol_stock (
      _w_id, _d_id, i_id_6, s_w_id_6, qty_6, ol_a_6,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_6); 

  ol_stock (
      _w_id, _d_id, i_id_7, s_w_id_7, qty_7, ol_a_7,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_7); 

  ol_stock (
      _w_id, _d_id, i_id_8, s_w_id_8, qty_8, ol_a_8,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_8); 

  ol_stock (
      _w_id, _d_id, i_id_9, s_w_id_9, qty_8, ol_a_9,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_9); 

  ol_stock (
      _w_id, _d_id, i_id_10, s_w_id_10, qty_10, ol_a_10,
      s_dist_01, s_dist_02, s_dist_03, s_dist_04, s_dist_05,
      s_dist_06, s_dist_07, s_dist_08, s_dist_09, s_dist_10, disti_10); 

  cust_info (_w_id, _d_id, _c_id, _c_last, _c_discount, _c_credit); 

  declare  d_cur cursor for
    select d_tax, d_next_o_id
      from tpcc..district
      where d_w_id = _w_id
        and d_id = _d_id; 

  whenever not found goto noware; 
  open d_cur (exclusive); 
  fetch d_cur into _d_tax, _o_id; 
  update tpcc..district set
    d_next_o_id = _o_id + 1
    where current of d_cur; 
  close d_cur; 

  insert into tpcc..orders (
      o_id, o_d_id, o_w_id, o_c_id, o_entry_d, o_ol_cnt, o_all_local)
    values (
      _o_id, _d_id, _w_id, _c_id, _datetime, o_ol_cnt, o_all_local); 

  insert into tpcc..new_order (no_o_id, no_d_id, no_w_id)
    values (_o_id, _d_id, _w_id); 

  select w_tax into _w_tax
    from tpcc..warehouse
    where w_id = _w_id; 
  
  tax_and_discount := (1 + _d_tax + _w_tax) * (1 - _c_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      1, i_id_1, qty_1, ol_a_1,  s_w_id_1, disti_1, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      2, i_id_2, qty_2, ol_a_2,  s_w_id_2, disti_2, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      3, i_id_3, qty_3, ol_a_3,  s_w_id_3, disti_3, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      4, i_id_4, qty_4, ol_a_4,  s_w_id_4, disti_4, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      5, i_id_5, qty_5, ol_a_5,  s_w_id_5, disti_5, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      6, i_id_6, qty_6, ol_a_6,  s_w_id_6, disti_6, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      7, i_id_7, qty_7, ol_a_7,  s_w_id_7, disti_7, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      8, i_id_6, qty_8, ol_a_8,  s_w_id_8, disti_8, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      9, i_id_9, qty_9, ol_a_9,  s_w_id_9, disti_9, tax_and_discount); 

  ol_insert (_w_id, _d_id, _o_id,
      10, i_id_10, qty_10, ol_a_10,  s_w_id_10, disti_10, tax_and_discount); 

  end_result ();
  result (_w_tax, _d_tax, _o_id, _c_last, _c_discount, _c_credit);
  return; 

noware:
  signal ('NOWRE', 'Warehouse or districtnot found.'); 
}
