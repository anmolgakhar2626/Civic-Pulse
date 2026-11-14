import React from 'react';
import { useAuth } from '../context/AuthContext';
import AdminDashboardView from '../components/dashboards/AdminDashboardView';
import CommandOperatorDashboardView from '../components/dashboards/CommandOperatorDashboardView';
import DepartmentOfficerDashboardView from '../components/dashboards/DepartmentOfficerDashboardView';
import FieldWorkerDashboardView from '../components/dashboards/FieldWorkerDashboardView';
import CitizenDashboardView from '../components/dashboards/CitizenDashboardView';

export const DashboardPage = () => {
  const { user, getPrimaryRole } = useAuth();
  const role = getPrimaryRole();

  switch (role) {
    case 'ADMIN':
      return <AdminDashboardView user={user} />;
    case 'COMMAND_OPERATOR':
      return <CommandOperatorDashboardView user={user} />;
    case 'DEPARTMENT_OFFICER':
      return <DepartmentOfficerDashboardView user={user} />;
    case 'FIELD_WORKER':
      return <FieldWorkerDashboardView user={user} />;
    case 'CITIZEN':
      return <CitizenDashboardView user={user} />;
    default:
      return <CommandOperatorDashboardView user={user} />;
  }
};

export default DashboardPage;
