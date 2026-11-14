import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';

export default function PublicLayout() {
  return (
    <div className="public-site-wrapper">
      <Navbar />
      <main className="public-main-content">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

