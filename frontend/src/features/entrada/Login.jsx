import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { googleLogin } from '../../api/auth';
import { readIntention, useSession } from '../../app/session';
import { Alert, Card, ErrorState, Icon } from '../../components/ui';

export default function Login() {
  const { user, loading, error, reload } = useSession();
  const [params] = useSearchParams();
  const [redirecting, setRedirecting] = useState(false);
  const navigate = useNavigate();
  useEffect(() => {
    if (user) navigate(readIntention(), { replace: true });
  }, [user, navigate]);
  return <main className="login-page">
    <Card className="login-card">
      <span className="brand-icon"><Icon name="marca" size={30} /></span>
      <div className="login-brand">DoceUp</div>
      <h1>Controle seus doces, vendas e dinheiro</h1>
      <p>Seu estoque e seu saldo em um só lugar, do primeiro ingrediente à última venda.</p>
      <a className="button button-primary google-login" href={googleLogin}
        onClick={(event) => redirecting ? event.preventDefault() : setRedirecting(true)}
        aria-busy={redirecting || loading}>
        {redirecting ? 'Abrindo o Google…' : 'Entrar com Google'}
      </a>
      <p className="field-help">Seus dados ficam separados por conta.</p>
      {(params.has('error') || params.has('erro')) && <Alert tone="error"><p>A entrada pelo Google falhou. Tente novamente.</p></Alert>}
      <ErrorState error={error} retry={reload} />
    </Card>
  </main>;
}
