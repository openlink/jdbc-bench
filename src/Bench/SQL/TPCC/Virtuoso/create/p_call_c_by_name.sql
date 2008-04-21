create procedure call_c_by_name (
    in w_id integer,
    in d_id integer,
    in c_last varchar)
{
  declare c_id integer; 

  c_by_name (w_id, d_id, c_last, c_id); 
}
