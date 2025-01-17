<%@page pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="html" uri="http://struts.apache.org/tags-html"%>
<%@taglib prefix="bean" uri="http://struts.apache.org/tags-bean"%>
<%@taglib prefix="tiles" uri="http://jakarta.apache.org/struts/tags-tiles"%>
<%@taglib prefix="s" uri="http://sastruts.seasar.org" %>
<%@taglib prefix="f" uri="http://sastruts.seasar.org/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="ja">
<head>
	<c:import url="/WEB-INF/view/common/head_inc.jsp" />

	<link href="${f:url('/css/ledger/property.css')}" media="all" rel="stylesheet" type="text/css" >
	<title>JPMC 社内システムサイト 物件検索</title>
</head>
<body>
	<div id="contents">

		<!-- header start -->
		<c:import url="/WEB-INF/view/common/header.jsp" />
		<!-- header end -->

		<!-- main_menu start -->
		<c:import url="/WEB-INF/view/common/menu.jsp" />
		<!-- main_menu stop -->

		<!-- content_main start -->
		<div id="content_main">
			<dl>
				<dt><img src="${f:url('/images/mark2.gif')}" width="13px" height="15px" class="img_mark">物件検索</dt>
				<dd>
					<div class="header_title"><p>検索条件</p></div>

					<s:form>
						<html:hidden property="p" styleId="p" />

						<table class="comm_tbl comm_tbl_center">
							<tr>
								<td class="tbl_header txt_10 td_top">物件名</td>
								<td class="td_top">
									<html:text property="search_property_name" styleClass="txt_20 ime_active" errorStyleClass="txt_20 ime_active input_error" />
									<html:errors property="search_property_name" />
								</td>
							</tr>
							<tr>
								<td class="tbl_header txt_10">加盟店名</td>
								<td>
									<html:text property="search_partner_name" styleClass="txt_20 ime_active" errorStyleClass="txt_20 ime_active input_error" />
									<html:errors property="search_partner_name" />
								</td>
							</tr>
							<tr>
								<td class="tbl_header">都道府県</td>
								<td>
									<html:select property="search_pref" styleClass="txt_9">
										<option value=""></option>
										<html:options collection="prefList" property="prefId" labelProperty="name" />
									</html:select>
									&nbsp;
									<html:errors property="search_pref" />
								</td>
							</tr>
							<tr>
								<td class="tbl_header txt_10">住所</td>
								<td>
									<html:text property="search_addr" styleClass="txt_20 ime_active" errorStyleClass="txt_20 ime_active input_error" maxlength="200" />
									<html:errors property="search_addr" />
								</td>
							</tr>
							<tr>
								<td class="tbl_header">PM/CO担当者</td>
								<td>
									<html:select property="search_staff" styleClass="txt_9">
										<option value=""></option>
										<html:options collection="staffList" property="staffId" labelProperty="name" />
									</html:select>
									<html:errors property="search_staff" />
								</td>
							</tr>
							<tr>
								<td class="tbl_header">種別</td>
								<td>
									<c:forEach items='${classList}' var='item'>
										<label><html:multibox property="search_class" value="${item.nameId}" />&nbsp;${f:h(item.name)}</label>&nbsp;
									</c:forEach>
								</td>
							</tr>
							<tr>
								<td class="tbl_header">取扱区分</td>
								<td>
									<label><html:checkbox property="search_type" />&nbsp;管理外も表示</label>&nbsp;
								</td>
								<!-- <td>
									<c:forEach items='${typeList}' var='item'>
										<label><html:checkbox property="search_type" />&nbsp;管理外も表示</label>
									</c:forEach>
								</td> -->
							</tr>
							<tr>
								<td class="tbl_header">新マスタへの移行</td>
								<td>
									<c:forEach items='${pptDataTypeList}' var='item'>
										<label><html:multibox property="serach_ppt_data_type" value="${item.key}" />&nbsp;${f:h(item.value)}</label>&nbsp;
									</c:forEach>
								</td>
							</tr>
							<tr>
								<td colspan="2" class="submit_area td_bottom">
									<s:submit styleClass="btn_100" property="search" value="検　索" onclick="$('#p').val('0');" />
								</td>
							</tr>
						</table>
					</s:form>

<c:if test="${searchListCnt >= 0}">

					<div class="header_title">
						<input type="button" value="新規登録" class="btn_150 btn_new" onclick="location.href='${f:url('/ledger/property/')}'">
						<p>検索結果</p>
					</div>

					<div class="pager property_list_pager">

						<c:if test="${pagerHasPrev}">
							<a href="search?p=${p - 1}${search_params}">&lt;&lt;</a>
						</c:if>

						&nbsp;${pagerFrom}-${pagerTo} 件 / ${pagerTotal} 件中&nbsp;

						<c:if test="${pagerHasNext}">
							<a href="search?p=${p + 1}${search_params}">&gt;&gt;</a>
						</c:if>

					</div>

					<table class="comm_tbl comm_tbl_center" id="property_list_tbl">
						<tr>
							<td class="tbl_header txt_center td_kbn">移<br>行</td>
							<td class="tbl_header txt_center td_class">種別</td>
							<td class="tbl_header txt_center td_property_name">物件名</td>
							<td class="tbl_header txt_center td_addr">住所</td>
							<td class="tbl_header txt_center td_type">取扱<br>区分</td>
							<td class="tbl_header txt_center td_pm_staff">PM担当</td>
							<td class="tbl_header txt_center td_co_staff">CO担当</td>
							<td class="tbl_header txt_center td_partner_name">加盟店名</td>
							<td class="tbl_header txt_center td_func"></td>
						</tr>

					<c:forEach var="item" items="${itemList}" varStatus="status">

						<c:choose>
							<c:when test="${status.count % 2 == 0}"><tr class="even_row"></c:when>
							<c:otherwise><tr></c:otherwise>
						</c:choose>
							<td class="td_kbn">
								${ f:h( item.pptDataTypeFlg ) }
							</td>
							<td class="td_class">
								${f:br(f:h(item.propertyType))}
							</td>
							<td class="td_property_name">
								${f:h(item.propertyName)} ${f:h(item.blockCd)}
							</td>
							<td class="td_addr">
								<c:if test="${not empty item.zipCode1}">
								〒${f:h(item.zipCode1)} - ${f:h(item.zipCode2)}<br>
								</c:if>
								${f:h(item.prefName)}${f:h(item.addr1)}${f:h(item.addr2)}
							</td>
							<td class="td_type">
								${f:label( item.handlingFlg, typeList, 'key', 'value' )}
							</td>
							<td class="td_pm_staff">
								${f:h(item.pmStaffName)}
							</td>
							<td class="td_co_staff">
								${f:h(item.coStaffName)}
							</td>
							<td class="td_partner_name">
								JP : ${f:h(item.jpPartnerName)}<br>
								CP : ${f:h(item.cpPartnerName)}<br>
								RP : ${f:h(item.rpPartnerName)}
							</td>
							<td class="td_func">
								<input type="button" class="btn_60" value="編集" onclick="location.href='${f:url('/ledger/property/update/')}?property_id=${item.propertyId}'">
							</td>
						</tr>

					</c:forEach>

						<tr>
							<td colspan="9" class="td_bottom"></td>
						</tr>
					</table>

</c:if>

				</dd>
			</dl>
		</div>
		<!-- content_main end -->

		<!-- footer start -->
		<c:import url="/WEB-INF/view/common/footer.jsp" />
		<!-- footer end -->
	</div>
</body>
</html>
