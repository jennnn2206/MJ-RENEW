import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getFirestore, collection, addDoc, getDocs } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";
const firebaseConfig = {
    apiKey: "AIzaSyARNQPIPkFuF8FUD629LhCDcJt_IoOq6M0",
    authDomain: "mj-renew.firebaseapp.com",
    projectId: "mj-renew",
    storageBucket: "mj-renew.firebasestorage.app",
    messagingSenderId: "396482481618",
    appId: "1:396482481618:web:5a48ad19ba5223c7f44c0b",
    measurementId: "G-EETCXNBB98"
  };
  
  const app = initializeApp(firebaseConfig);
  const auth = getAuth(app);
  const db = getFirestore(app);
  
  // Navegación
  function showSection(section) {
    document.querySelectorAll(".container > div").forEach(div => {
      div.classList.add("hidden");
    });
    document.getElementById(section).classList.remove("hidden");
  }
  
  // Registro
  window.register = function () {
    let email = document.getElementById("email").value;
    let password = document.getElementById("password").value;
  
    createUserWithEmailAndPassword(auth, email, password)
      .then(() => alert("Registrado correctamente"))
      .catch(err => alert(err.message));
  };
  
  // Login
  window.login = function () {
    let email = document.getElementById("email").value;
    let password = document.getElementById("password").value;
  
    signInWithEmailAndPassword(auth, email, password)
      .then(() => {
        alert("Bienvenido");
        showSection("market");
        loadItems();
      })
      .catch(err => alert(err.message));
  };
  
  // Agregar mueble
  window.addItem = async function () {
    let name = document.getElementById("name").value;
    let price = document.getElementById("price").value;
    let image = document.getElementById("image").value;
  
    await addDoc(collection(db, "muebles"), {
      name,
      price,
      image
    });
  
    alert("Mueble agregado");
    loadItems();
  };
  
  // Cargar muebles
  async function loadItems() {
    const container = document.getElementById("items");
    container.innerHTML = "";
  
    const querySnapshot = await getDocs(collection(db, "muebles"));
  
    querySnapshot.forEach((doc) => {
      let item = doc.data();
  
      container.innerHTML += `
        <div class="card">
          <img src="${item.image}" />
          <h3>${item.name}</h3>
          <p>$${item.price}</p>
        </div>
      `;
    });
  }
  
  // Simulación IA (placeholder para API real)
  function simulateAI() {
    let condition = document.getElementById("condition").value;
    let furniture = document.getElementById("furniture").value;
  
    let cost = (10 - condition) * 700;
    let time = (10 - condition) * 3;
  
    document.getElementById("result").innerHTML = `
      <div class="card">
        <h3>${furniture}</h3>
        <p>Costo IA estimado: $${cost}</p>
        <p>Tiempo estimado: ${time} días</p>
        <p>Valor incrementado proyectado</p>
      </div>
    `;
  }
  