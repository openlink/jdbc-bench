create procedure ol_insert (
    inout w_id integer,
    inout d_id integer,
    inout o_id integer,
    in ol_number integer,
    inout ol_i_id integer,
    inout ol_qty integer,
    inout ol_amount float,
    inout ol_supply_w_id integer,
    inout ol_dist_info varchar,
    inout tax_and_discount float)
{
  if (ol_i_id = -1) return; 
  ol_amount := ol_amount * tax_and_discount;

  insert into tpcc..order_line (
      ol_o_id, ol_d_id, ol_w_id, ol_number, ol_i_id, ol_supply_w_id,
      ol_quantity, ol_amount, ol_dist_info) 
    values (
      o_id, d_id, w_id, ol_number, ol_i_id, ol_supply_w_id,
      ol_qty, ol_amount, ol_dist_info); 
}
