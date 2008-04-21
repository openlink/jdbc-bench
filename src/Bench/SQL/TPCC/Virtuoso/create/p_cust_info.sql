create procedure cust_info (
    in w_id integer,
    in d_id integer,
    inout _c_id integer,
    inout _c_last varchar,
    out _c_discount float,
    out _c_credit varchar)
{
  whenever not found goto err; 
  select c_last, c_discount, c_credit into _c_last, _c_discount, _c_credit
    from tpcc..customer
    where c_w_id = w_id
      and c_d_id = d_id
      and c_id = _c_id; 
  return; 

err:
  signal ('BOCUS', 'No customer'); 
}
