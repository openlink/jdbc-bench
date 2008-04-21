create procedure c_by_name (
    in w_id integer,
    in d_id integer,
    in name varchar,
    out id integer)
{
  declare n, c_count integer; 
  declare c_cur cursor for
    select c_id
      from tpcc..customer
      where c_w_id = w_id
       and c_d_id = d_id
       and c_last = name
      order by c_w_id, c_d_id, c_last, c_first; 

  select count (*) into c_count
    from tpcc..customer
    where c_w_id = w_id
      and c_d_id = d_id
      and c_last = name; 

  n := 0; 
  open c_cur; 
  whenever not found goto notfound; 
  while (n <= c_count / 2)
    {
      fetch c_cur into id; 
      n := n + 1; 
    }
  return; 

notfound:
  signal ('cnf', 'customer not found by name'); 
  return; 
}
