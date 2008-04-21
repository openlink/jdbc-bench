create procedure bc_c_data (
    inout c_new varchar,
    inout c_data varchar)
{
  return concatenate (c_new, subseq (c_data, length (c_new), length (c_data)));
}
