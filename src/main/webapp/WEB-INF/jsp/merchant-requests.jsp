<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <title>PayOn Admin - Merchant Requests</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f4f6f9;
            margin: 30px;
        }
        .container {
            background: #fff;
            padding: 25px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        h2 {
            color: #2c3e50;
            border-bottom: 2px solid #3498db;
            padding-bottom: 10px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 12px;
            text-align: right;
            border-bottom: 1px solid #ddd;
        }
        th {
            background-color: #34495e;
            color: white;
        }
        .btn-details {
            padding: 6px 14px;
            background: #3498db;
            color: white;
            border-radius: 4px;
            text-decoration: none;
            font-weight: bold;
            font-size: 13px;
            display: inline-block;
        }
        .btn-details:hover {
            background: #2980b9;
        }
        .btn-logout {
            padding: 8px 18px;
            background: #e74c3c;
            color: white;
            border: none;
            border-radius: 5px;
            font-weight: bold;
            cursor: pointer;
            font-size: 14px;
        }
        .btn-logout:hover {
            background: #c0392b;
        }
        .badge-pending {
            background: #f39c12;
            color: white;
            padding: 4px 8px;
            border-radius: 4px;
            font-size: 12px;
        }
    </style>
</head>
<body>

<div class="container">
    <h2>Enterprise Pending Requests</h2>

    <form action="${pageContext.request.contextPath}/logout" method="post">
        <button type="submit" class="btn-logout">Logout</button>
    </form>

    <c:if test="${param.msg eq 'approved'}">
        <p style="color: #27ae60; font-weight: bold;">The merchant has been approved, wallet activated, and keys delivered!</p>
    </c:if>
    <c:if test="${param.msg eq 'rejected'}">
        <p style="color: #c0392b; font-weight: bold;">The request has been rejected and deleted successfully.</p>
    </c:if>

    <table>
        <thead>
        <tr>
            <th>ID</th>
            <th>Company Name</th>
            <th>Business Email</th>
            <th>Status</th>
            <th>Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="merchant" items="${pendingMerchants}">
            <tr>
                <td>#${merchant.merchantId}</td>
                <td><strong>${merchant.merchantUserName}</strong></td>
                <td>${merchant.user.email}</td>
                <td><span class="badge-pending">${merchant.enterpriseMerchantStatus}</span></td>
                <td>
                    <a href="${pageContext.request.contextPath}/admin/merchant/${merchant.merchantId}" class="btn-details">View Details & Action</a>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty pendingMerchants}">
            <tr>
                <td colspan="5" style="text-align: center; color: #7f8c8d; padding: 20px;">
                    There's no pending requests to review.
                </td>
            </tr>
        </c:if>
        </tbody>
    </table>
</div>

</body>
</html>