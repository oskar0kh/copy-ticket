import React from "react";
import { createRoot } from "react-dom/client";
import Login from "./main_page/Login";
import "./main_page/css/styles.css";

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <Login />
  </React.StrictMode>
);
