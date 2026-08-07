<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
    <meta charset="UTF-8">
    <title>PayOn - طلب انضمام Enterprise Merchant</title>
    <style>
        * {
            box-sizing: border-box;
            font-family: 'Segoe UI',
            Tahoma, Geneva, Verdana,
            sans-serif; }
        body {
            background-color: #f0f2f5;
            margin: 0;
            padding: 40px 20px; }
        .form-card {
            max-width: 600px;
            margin: 0 auto;
            background: #fff;
            padding: 30px;
            border-radius: 10px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
        h2 {
            color: #1a252f;
            margin-bottom: 20px;
            text-align: center; }
        .form-group {
            margin-bottom: 15px; }
        label {
            display: block;
            margin-bottom: 6px;
            font-weight: bold;
            color: #333; }
        input[type="text"],
        input[type="email"],
        input[type="url"] {
            width: 100%;
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 6px;
            font-size: 14px; }
        .btn-submit {
            width: 100%;
            background: #27ae60;
            color: white;
            border: none; padding: 12px;
            border-radius: 6px;
            font-size: 16px;
            cursor: pointer;
            font-weight: bold; }
        .btn-submit:hover {
            background: #219150; }
        .alert-success {
            background: #d4edda;
            color: #155724;
            padding: 10px;
            border-radius: 6px;
            margin-bottom: 15px; }
    </style>
</head>
<body>

<div class="form-card">
    <h2>Enterprise Merchant Join Request :-</h2>

    <c:if test="${not empty successMessage}">
        <div class="alert-success">${successMessage}</div>
    </c:if>

    <form action="${pageContext.request.contextPath}/api/v1/enterprise/merchant/form" method="post">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="form-group">
            <label>Company Name: </label>
            <input type="text" name="companyName" required />
        </div>
        <div class="form-group">
            <label>Business Email: </label>
            <input type="email" name="email" required />
        </div>
        <div class="form-group">
            <label>Commercial Registration Number: </label>
            <input type="text" name="taxId" required />
        </div>
        <div class="form-group">
            <label>Phone Number: </label>
            <input type="text" name="phoneNumber" required />
        </div>
        <div class="form-group">
            <label>Response Url: </label>
            <input type="url" name="webhookUrl" placeholder="https://api.yourcompany.com/payon-webhook" required />
        </div>
        <button type="submit" class="btn-submit">Submit for Review</button>
        </form>
</div>
</body>
</html>