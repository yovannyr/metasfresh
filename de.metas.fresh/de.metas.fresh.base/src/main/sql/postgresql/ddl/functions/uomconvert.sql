create or replace function uomConvert
(
	p_M_Product_ID numeric
	, p_C_UOM_From_ID numeric
	, p_C_UOM_To_ID numeric
	, p_Qty numeric
)
returns numeric
as
$BODY$
declare
	v_Product_UOM_ID numeric;
	v_Precision numeric;
	v_QtyConv numeric;
begin
	-- Try converting using the direct conversion (if exists)
	v_QtyConv := uomConvertDirect(p_M_Product_ID, p_C_UOM_From_ID, p_C_UOM_To_ID, p_Qty);

	-- Try converting using product's UOM as intermediate UOM
	if (v_QtyConv is null) then
		-- Fetch product's UOM and precision
		select p.C_UOM_ID
		into v_Product_UOM_ID
		from M_Product p
		where p.M_Product_ID=p_M_Product_ID;

		-- Convert from p_C_UOM_From_ID to Product UOM
		v_QtyConv := uomConvertDirect(p_M_Product_ID, p_C_UOM_From_ID, v_Product_UOM_ID, p_Qty);
		-- Convert from Product UOM to p_C_UOM_To_ID
		v_QtyConv := uomConvertDirect(p_M_Product_ID, v_Product_UOM_ID, p_C_UOM_To_ID, v_QtyConv);
	end if;

	--
	-- Round to precision
	select u.StdPrecision into v_Precision from C_UOM u where u.C_UOM_ID=p_C_UOM_To_ID;
	if (v_Precision is not null) then
		v_QtyConv := round(v_QtyConv, v_Precision);
	end if;

	return v_QtyConv;
end;
$BODY$
LANGUAGE plpgsql VOLATILE
COST 100;
ALTER FUNCTION uomConvert(numeric, numeric, numeric, numeric) OWNER TO adempiere;
