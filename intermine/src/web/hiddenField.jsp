<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- hiddenField.jsp -->
<div>
  <nobr>
    <span class="fieldName"><c:out value="${fieldDescriptor.name}"/></span>:
    <c:if test="${object[fieldDescriptor.name] == null}">
      <fmt:message key="objectDetails.nullField"/>
    </c:if>
    <c:if test="${object[fieldDescriptor.name] != null}">
      <fmt:message key="hidden.field"/>
    </c:if>
  </nobr>
</div>
<!-- /hiddenField.jsp -->
