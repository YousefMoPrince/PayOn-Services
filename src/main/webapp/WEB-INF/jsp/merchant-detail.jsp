<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <title>PayOn Admin - تفاصيل التاجر</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f4f6f9;
            margin: 30px; }
        .card {
            max-width: 700px;
            margin: 0 auto;
            background: #fff;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        h2 {
            color: #2c3e50;
            border-bottom: 2px solid #3498db;
            padding-bottom: 10px;
            margin-top: 0; }
        .detail-row
        { display: flex;
            justify-content: space-between;
            padding: 12px 0;
            border-bottom: 1px solid #eee; }
        .label {
            font-weight: bold;
            color: #555; }
        .value {
            color: #222; }
        .actions {
            margin-top: 25px;
            display: flex;
            gap: 10px;
            justify-content: flex-end; }
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-weight: bold;
            text-decoration: none;
            text-align: center; }
        .btn-approve {
            background: #2ecc71;
            color: white;
        }
        .btn-delete {
            background: #e74c3c;
            color: white; }
        .btn-back {
            background: #7f8c8d;
            color: white; }
        .badge {
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: bold; }
        .badge-pending {
            background: #f39c12;
            color: white; }
    </style>
</head>
<body>

<div class="card">
    <h2>Merchant Request Details (#${merchant.merchantId})</h2>

    <div class="detail-row">
        <span class="label">Company Name: </span>
        <span class="value">${merchant.merchantUserName}</span>
    </div>
    <div class="detail-row">
        <span class="label">Business Email: </span>
        <span class="value">${merchant.user.email}</span>
    </div>
    <div class="detail-row">
        <span class="label">Commercial Registration Number: </span>
        <span class="value">${merchant.taxId}</span>
    </div>
    <div class="detail-row">
        <span class="label">Phone Number: </span>
        <span class="value">${merchant.user.phoneNumber}</span>
    </div>
    <div class="detail-row">
        <span class="label">Response Url: </span>
        <span class="value" dir="ltr"><code>${merchant.merchantUrl}</code></span>
    </div>
    <div class="detail-row">
        <span class="label">Current Request status:</span>
        <span class="value"><span class="badge badge-pending">${merchant.enterpriseMerchantStatus}</span></span>
    </div>

    <div class="actions">
        <a href="${pageContext.request.contextPath}/admin/pending" class="btn btn-back">Back to Requests</a>

        <form action="${pageContext.request.contextPath}/admin/${merchant.merchantId}/reject" method="post" style="display:inline;">
            <button type="submit" class="btn btn-delete" onclick="return confirm('Are you sure you want to reject and delete the request?')">reject and delete</button>
        </form>

        <form action="${pageContext.request.contextPath}/admin/${merchant.merchantId}/approve" method="post" style="display:inline;">
            <button type="submit" class="btn btn-approve">Key activation and generation</button>
        </form>
    </div>
</div>

</body>
</html>