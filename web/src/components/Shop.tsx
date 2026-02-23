import { useEffect, useState } from 'react'
import { useAuthStore } from '../stores/authStore'
import './Shop.css'

interface ShopItem {
  id: string
  name: string
  description: string
  type: 'AVATAR_FRAME' | 'BADGE' | 'THEME' | 'TITLE' | 'EMOTE' | 'BOOST'
  price: number
  imageUrl?: string
  levelRequired?: number
  owned: boolean
  equipped: boolean
}

export default function Shop() {
  const { user } = useAuthStore()
  const [items, setItems] = useState<ShopItem[]>([])
  const [inventory, setInventory] = useState<ShopItem[]>([])
  const [activeTab, setActiveTab] = useState<'shop' | 'inventory'>('shop')
  const [selectedType, setSelectedType] = useState<string>('ALL')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchItems()
    fetchInventory()
  }, [])

  const fetchItems = async () => {
    try {
      const response = await fetch('/api/shop/items', {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (response.ok) {
        const data = await response.json()
        setItems(data)
      }
    } catch (error) {
      console.error('Failed to fetch shop items:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchInventory = async () => {
    try {
      const response = await fetch('/api/shop/inventory', {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (response.ok) {
        const data = await response.json()
        setInventory(data.map((inv: any) => inv.shopItem))
      }
    } catch (error) {
      console.error('Failed to fetch inventory:', error)
    }
  }

  const purchaseItem = async (itemId: string) => {
    try {
      const response = await fetch(`/api/shop/items/${itemId}/purchase`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (response.ok) {
        fetchItems()
        fetchInventory()
      }
    } catch (error) {
      console.error('Failed to purchase item:', error)
    }
  }

  const equipItem = async (itemId: string) => {
    try {
      const response = await fetch(`/api/shop/items/${itemId}/equip`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
      })
      if (response.ok) {
        fetchInventory()
      }
    } catch (error) {
      console.error('Failed to equip item:', error)
    }
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'AVATAR_FRAME': return '🖼️'
      case 'BADGE': return '🏅'
      case 'THEME': return '🎨'
      case 'TITLE': return '👑'
      case 'EMOTE': return '😀'
      case 'BOOST': return '🚀'
      default: return '📦'
    }
  }

  const itemTypes = ['ALL', 'AVATAR_FRAME', 'BADGE', 'THEME', 'TITLE', 'EMOTE', 'BOOST']

  const filteredItems = activeTab === 'shop'
    ? items.filter(item => selectedType === 'ALL' || item.type === selectedType)
    : inventory.filter(item => selectedType === 'ALL' || item.type === selectedType)

  if (loading) return <div className="shop-loading">Loading shop...</div>

  return (
    <div className="shop-container">
      <div className="shop-header">
        <h2>Shop</h2>
        <div className="user-currency">
          <span>💰 {user?.points || 0} Points</span>
        </div>
      </div>

      <div className="shop-tabs">
        <button
          className={activeTab === 'shop' ? 'active' : ''}
          onClick={() => setActiveTab('shop')}
        >
          Shop
        </button>
        <button
          className={activeTab === 'inventory' ? 'active' : ''}
          onClick={() => setActiveTab('inventory')}
        >
          My Items
        </button>
      </div>

      <div className="type-filter">
        {itemTypes.map(type => (
          <button
            key={type}
            className={selectedType === type ? 'active' : ''}
            onClick={() => setSelectedType(type)}
          >
            {getTypeIcon(type)} {type === 'ALL' ? 'All' : type.replace('_', ' ')}
          </button>
        ))}
      </div>

      <div className="shop-grid">
        {filteredItems.length === 0 ? (
          <div className="no-items">No items found</div>
        ) : (
          filteredItems.map(item => (
            <div key={item.id} className="shop-item">
              <div className="item-image">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} />
                ) : (
                  <span className="item-placeholder">{getTypeIcon(item.type)}</span>
                )}
              </div>
              <div className="item-info">
                <h3>{item.name}</h3>
                <p>{item.description}</p>
                <span className="item-type">{getTypeIcon(item.type)} {item.type}</span>
                {item.levelRequired && (
                  <span className="level-requirement">
                    Level {item.levelRequired} required
                  </span>
                )}
              </div>
              <div className="item-actions">
                {activeTab === 'shop' ? (
                  <button
                    className="purchase-btn"
                    onClick={() => purchaseItem(item.id)}
                    disabled={item.owned || (item.levelRequired && (user?.level || 0) < item.levelRequired)}
                  >
                    {item.owned ? 'Owned' : `💰 ${item.price}`}
                  </button>
                ) : (
                  <button
                    className={`equip-btn ${item.equipped ? 'equipped' : ''}`}
                    onClick={() => equipItem(item.id)}
                  >
                    {item.equipped ? 'Equipped ✓' : 'Equip'}
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
