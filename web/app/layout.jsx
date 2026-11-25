import './styles.css';

export const metadata = {
  title: 'Taller Manager',
  description: 'Agenda, órdenes de servicio e inventario para un taller mecánico.',
  robots: { index: false, follow: false }
};

export default function Layout({ children }) {
  return <html lang="es-CO"><body>{children}</body></html>;
}
