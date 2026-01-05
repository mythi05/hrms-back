// Step 1: Login để lấy token mới
fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    username: 'admin',
    password: 'admin123'
  })
})
.then(res => res.json())
.then(data => {
  if (!data.token) {
    throw new Error('Login failed: no token received');
  }
  
  console.log('✅ LOGIN SUCCESS');
  console.log('Token:', data.token.substring(0, 50) + '...');
  
  // Step 2: Test /api/employees với token mới
  return fetch('http://localhost:8080/api/employees', {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ' + data.token,
      'Content-Type': 'application/json'
    }
  });
})
.then(res => {
  console.log('\n✅ /api/employees Status:', res.status);
  if (res.status === 200) {
    return res.json().then(data => {
      console.log('Employees count:', Array.isArray(data) ? data.length : 'unknown');
      console.log('Response:', JSON.stringify(data).substring(0, 200) + '...');
    });
  } else if (res.status === 401) {
    console.log('❌ 401 Unauthorized - token not accepted');
    return res.json().then(err => console.log('Error:', err));
  } else {
    console.log('Status:', res.status);
    return res.text().then(text => console.log('Response:', text));
  }
})
.catch(err => {
  console.log('❌ Error:', err.message);
});
